package coverosR3z.persistence

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.*
import coverosR3z.config.CURRENT_DATABASE_VERSION
import coverosR3z.misc.*
import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.getTime
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializationKeys
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.databaseFileSuffix
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.timerecording.persistence.ITimeEntryPersistence
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.io.File
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

class PureMemoryDatabaseTests {

    private lateinit var pmd: PureMemoryDatabase

    @Before
    fun init() {
        pmd = PureMemoryDatabase()
    }

    @After
    fun cleanup() {
        pmd.stop()
    }

    /**
     * This will typically complete in 40 milliseconds
     */
    @Category(PerformanceTestCategory::class)
    @Test
    fun `should get responses from the database quickly_PERFORMANCE`() {
        val numberOfEmployees = 30
        val numberOfProjects = 30
        val numberOfDays = 31

        val tep = TimeEntryPersistence(pmd, logger = testLogger)
        val allEmployees = recordManyTimeEntries(tep, numberOfEmployees, numberOfProjects, numberOfDays)


        val (totalTime) = getTime {
            readTimeEntriesForOneEmployee(tep, allEmployees)
            accumulateMinutesPerEachEmployee(tep, allEmployees)
        }

        testLogger.logAudit { "It took a total of $totalTime milliseconds for this code" }
        File("${granularPerfArchiveDirectory}should_get_responses_from_the_database_quickly_PERFORMANCE")
            .appendText("${Date.now().stringValue}\tnumberOfEmployees: $numberOfEmployees\tnumberProjects: $numberOfProjects\tnumberOfDays: $numberOfDays\ttime: $totalTime\n")
    }

    /**
     * I wish to make an exact copy of the PMD in completely new memory locations
     */
    @Test
    fun testShouldBePossibleToCopy_different() {
        val originalPmd = pmd.copy()
        pmd.EmployeeDataAccess().actOn { it.add(DEFAULT_EMPLOYEE) }
        assertNotEquals("after adding a new employee, the databases should differ", originalPmd, pmd)
    }

    /**
     * I wish to make an exact copy of the PMD in completely new memory locations
     */
    @Test
    fun testShouldBePossibleToCopy_similar() {
        val originalPmd = pmd.copy()
        assertEquals(originalPmd, pmd)
    }

    /**
     * Test writing the whole database and reading the whole database
     */
    @IntegrationTest(usesDirectory=true)
    @Category(PerformanceTestCategory::class)
    @Test
    fun testShouldWriteAndReadToDisk_PERFORMANCE() {
        val numberOfEmployees = 20
        val numberOfProjects = 10
        val numberOfDays = 31

        val dbDirectory = DEFAULT_DB_DIRECTORY + "testShouldWriteAndReadToDisk_PERFORMANCE/"
        File(dbDirectory).deleteRecursively()
        pmd = DatabaseDiskPersistence(dbDirectory, testLogger).startWithDiskPersistence()
        val tep = TimeEntryPersistence(pmd, logger = testLogger)

        val (totalTimeWriting, _) = getTime {
            recordManyTimeEntries(tep, numberOfEmployees, numberOfProjects, numberOfDays)
            pmd.stop()
        }

        val (totalTimeReading, readPmd) = getTime {
            DatabaseDiskPersistence(dbDirectory, testLogger).startWithDiskPersistence()
        }

        assertEquals("The database should be exactly the same after the reload", pmd, readPmd)

        val totalTime = totalTimeReading + totalTimeWriting
        File("${granularPerfArchiveDirectory}testShouldWriteAndReadToDisk_PERFORMANCE")
            .appendText("${Date.now().stringValue}\tnumberOfEmployees: $numberOfEmployees\tnumberOfProjects: $numberOfProjects\tnumberOfDays: $numberOfDays\ttotalTime: $totalTime\n")
        testLogger.logAudit { "Total time taken for serialization / deserialzation was $totalTime milliseconds" }
    }

    /**
     * This is to test that it could be possible to corrupt the data when
     * multiple threads are changing it
     *
     * The idea behind this test is that if we aren't handling
     * the threading cleanly, we won't get one hundred employees
     * added, it will be some smaller number.
     *
     * It used to be easy to see this fail, you just had to remove
     * the locking mechanism from the method at
     * PureMemoryDatabase.addNewEmployee, and you
     * would need to run it a time or two to see it fail.
     *
     * Now, however, we aren't using locking - we're using
     * [coverosR3z.persistence.types.ConcurrentSet] which is based on [ConcurrentHashMap], and
     * also [AtomicInteger], which means we don't need to lock
     * at all.
     *
     * So the only way to get this un-thread-safe again
     * would be to switch back to using an un-thread-safe
     * data collection, which isn't quite as easy.
     *
     * In any case, this test should still help us, since
     * if this test passes, it should still mean we are handling
     * our threads properly.
     */
    @Test
    fun testCorruptingEmployeeDataWithMultiThreading() {
        val tep = TimeEntryPersistence(pmd, logger = testLogger)
        val listOfThreads = mutableListOf<Future<*>>()
        val numberNewEmployeesAdded = 20
        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
        testLogger.turnOffAllLogging()
        repeat(numberNewEmployeesAdded) { // each thread calls the add a single time
            listOfThreads.add(cachedThreadPool.submit(Thread {
                tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
            }))
        }
        // wait for all those threads
        listOfThreads.forEach{it.get()}
        testLogger.resetLogSettingsToDefault()
        assertEquals(numberNewEmployeesAdded, tep.getAllEmployees().size)
    }


    /**
     * See [testCorruptingEmployeeDataWithMultiThreading]
     */
    @Test
    fun testCorruptingProjectDataWithMultiThreading() {
        val tep = TimeEntryPersistence(pmd, logger = testLogger)
        val listOfThreads = mutableListOf<Future<*>>()
        val numberNewProjectsAdded = 20
        testLogger.turnOffAllLogging()
        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
        repeat(numberNewProjectsAdded) { // each thread calls the add a single time
            listOfThreads.add(cachedThreadPool.submit(Thread {
                tep.persistNewProject(DEFAULT_PROJECT_NAME)
            }))
        }
        // wait for all those threads
        listOfThreads.forEach{it.get()}
        testLogger.resetLogSettingsToDefault()
        assertEquals(numberNewProjectsAdded, tep.getAllProjects().size)
    }

    /**
     * See [testCorruptingEmployeeDataWithMultiThreading]
     */
    @Test
    fun testCorruptingTimeEntryDataWithMultiThreading() {
        val tep = TimeEntryPersistence(pmd, logger = testLogger)
        val listOfThreads = mutableListOf<Future<*>>()
        val numberTimeEntriesAdded = 20
        val newProject = tep.persistNewProject(DEFAULT_PROJECT_NAME)
        val newEmployee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
        testLogger.turnOffAllLogging()
        repeat(numberTimeEntriesAdded) { // each thread calls the add a single time
            listOfThreads.add(cachedThreadPool.submit(Thread {
                tep.persistNewTimeEntry(createTimeEntryPreDatabase(project = newProject, employee = newEmployee))
            }))
        }
        // wait for all those threads
        listOfThreads.forEach{it.get()}
        testLogger.resetLogSettingsToDefault()
        assertEquals(numberTimeEntriesAdded, tep.readTimeEntries(DEFAULT_EMPLOYEE).size)
    }

    /**
     * If we're starting and the directory for the database exists but no
     * files or subdirectories in it.  everything should 
     * still work as before
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_MissingAllFilesButDirectoryExists() {
        val dbDirectory = DEFAULT_DB_DIRECTORY + "testPersistence_Read_MissingAllFilesButDirectoryExists/"
        File(dbDirectory).deleteRecursively()
        File(dbDirectory).mkdirs()
        DatabaseDiskPersistence(dbDirectory, testLogger).startWithDiskPersistence()
        assertTrue("a new database will store its version", File(dbDirectory + "currentVersion.txt").exists())
    }

    /**
     * If we are starting and try to read and there's no 
     * database directory.  Just make one.
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_MissingDbDirectory() {
        val dbDirectory = DEFAULT_DB_DIRECTORY + "testPersistence_Read_MissingDbDirectory/"
        File(dbDirectory).deleteRecursively()
        DatabaseDiskPersistence(dbDirectory, testLogger).startWithDiskPersistence()
        assertTrue("a new database will store its version", File(dbDirectory + "currentVersion.txt").exists())
    }

    /**
     * Assuming we have valid data stored on disk, do we read
     * it back properly?
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_HappyPath() {
        val readPmd = arrangeFullDatabaseWithDisk(databaseDirectorySuffix = "testPersistence_Read_HappyPath")

        assertEquals(pmd, readPmd)
        assertEquals(pmd.hashCode(), readPmd.hashCode())
    }

    /**
     * These tests capture what happens when a file doesn't exist in the directory
     * because no entries have been added, not because it's become corrupted.
     *
     * Here there are no users, so there cannot be any sessions either
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_MissingUsers() {
        val readPmd = arrangeFullDatabaseWithDisk(skipCreatingUser = true, databaseDirectorySuffix = "testPersistence_Read_MissingUsers")

        assertEquals(pmd, readPmd)
        assertEquals(pmd.hashCode(), readPmd.hashCode())
    }

    /**
     * These tests capture what happens when a file doesn't exist in the directory
     * because no entries have been added, not because it's become corrupted.
     *
     * Here there are no sessions, which might mean no one was logged in
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_MissingSessions() {
        val readPmd = arrangeFullDatabaseWithDisk(skipCreatingSession = true, databaseDirectorySuffix = "testPersistence_Read_MissingSessions")

        assertEquals(pmd, readPmd)
        assertEquals(pmd.hashCode(), readPmd.hashCode())
    }

    /**
     * This is similar to [testPersistence_Read_MissingSessions] but
     * where the sessions file is empty, instead of missing
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_EmptySessionsFile() {
        val databaseDirectorySuffix = "testPersistence_Read_EmptySessionsFile"
        val readPmd = arrangeFullDatabaseWithDisk(skipCreatingTimeEntries = true, databaseDirectorySuffix = databaseDirectorySuffix)

        // set the sessions file to empty
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Session.directoryName}$databaseFileSuffix").delete()
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Session.directoryName}$databaseFileSuffix").createNewFile()

        assertEquals(pmd, readPmd)
        assertEquals(pmd.hashCode(), readPmd.hashCode())
    }

    /**
     * These tests capture what happens when a file doesn't exist in the directory
     * because no entries have been added, not because it's become corrupted.
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_MissingTimeEntries() {
        val readPmd = arrangeFullDatabaseWithDisk(skipCreatingTimeEntries = true, databaseDirectorySuffix = "testPersistence_Read_MissingTimeEntries")

        assertEquals(pmd, readPmd)
        assertEquals(pmd.hashCode(), readPmd.hashCode())
    }

    /**
     * These tests capture what happens when a file doesn't exist in the directory
     * because no entries have been added, not because it's become corrupted.
     *
     * here there are no employees, so there cannot be any time entries
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_MissingEmployees() {
        val readPmd = arrangeFullDatabaseWithDisk(skipCreatingTimeEntries = true, databaseDirectorySuffix = "testPersistence_Read_MissingEmployees")

        assertEquals(pmd, readPmd)
        assertEquals(pmd.hashCode(), readPmd.hashCode())
    }


    /**
     * This is similar to [testPersistence_Read_MissingEmployees] but
     * where the file is empty, instead of missing
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_EmptyEmployees() {
        val databaseDirectorySuffix = "testPersistence_Read_EmptyEmployees"
        val readPmd = arrangeFullDatabaseWithDisk(skipCreatingTimeEntries = true, databaseDirectorySuffix = databaseDirectorySuffix)

        // set the file to empty
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Employee.directoryName}$databaseFileSuffix").delete()
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Employee.directoryName}$databaseFileSuffix").createNewFile()

        assertEquals(pmd, readPmd)
        assertEquals(pmd.hashCode(), readPmd.hashCode())
    }

    /**
     * These tests capture what happens when a file doesn't exist in the directory
     * because no entries have been, not because it's become corrupted.
     *
     * Here, there are no projects, which also means there cannot be time entries
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_MissingProjects() {
        val readPmd = arrangeFullDatabaseWithDisk(skipCreatingProjects = true, skipCreatingTimeEntries = true, databaseDirectorySuffix = "testPersistence_Read_MissingProjects")

        assertEquals(pmd, readPmd)
        assertEquals(pmd.hashCode(), readPmd.hashCode())
    }



    /**
     * This is similar to [testPersistence_Read_MissingProjects] but
     * where the file is empty, instead of missing
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_EmptyProjects() {
        val databaseDirectorySuffix = "testPersistence_Read_EmptyProjects"
        val readPmd = arrangeFullDatabaseWithDisk(skipCreatingTimeEntries = true, databaseDirectorySuffix = databaseDirectorySuffix)

        // set the file to empty
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Project.directoryName}$databaseFileSuffix").delete()
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Project.directoryName}$databaseFileSuffix").createNewFile()

        assertEquals(pmd, readPmd)
        assertEquals(pmd.hashCode(), readPmd.hashCode())
    }

    /**
     * What if some of the data in the time-entries file is
     * corrupted? I think the most appropriate
     * response is to halt and yell for help, because at that point all
     * bets are off, we won't have enough information to properly recover.
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_CorruptedData_TimeEntries_BadData() {
        val databaseDirectorySuffix = "testPersistence_Read_CorruptedData_TimeEntries_BadData"
        arrangeFullDatabaseWithDisk(skipRestarting = true, databaseDirectorySuffix = databaseDirectorySuffix)

        // corrupt the time-entries data file
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${TimeEntry.directoryName}/2$databaseFileSuffix").writeText("BAD DATA HERE")

        val ex = assertThrows(DatabaseCorruptedException::class.java) {
            DatabaseDiskPersistence(
                "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/", testLogger)
                .startWithDiskPersistence()
        }
        assertEquals("Unable to deserialize this text as time entry data: BAD DATA HERE", ex.message)
    }

    /**
     * See [testPersistence_Read_CorruptedData_TimeEntries_BadData]
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_CorruptedData_Employees_BadData() {
        val databaseDirectorySuffix = "testPersistence_Read_CorruptedData_Employees_BadData"
        arrangeFullDatabaseWithDisk(skipRestarting = true, databaseDirectorySuffix = databaseDirectorySuffix)

        // create a bogus corrupt employee id file
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Employee.directoryName}/27$databaseFileSuffix").writeText("BAD DATA HERE")
        
        val ex = assertThrows(DatabaseCorruptedException::class.java) {
            DatabaseDiskPersistence(
                "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/", testLogger)
                .startWithDiskPersistence()
        }
        assertEquals("Unable to deserialize this text from the employees directory: BAD DATA HERE", ex.message)
    }

    /**
     * What if we see corruption in our database by dint of missing files
     * that definitely should not be missing?  I think the most appropriate
     * response is to halt and yell for help, because at that point all
     * bets are off, we won't have enough information to properly recover.
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_CorruptedData_Employees_MissingFile() {
        val databaseDirectorySuffix = "testPersistence_Read_CorruptedData_Employees_MissingFile"
        arrangeFullDatabaseWithDisk(skipRestarting = true, databaseDirectorySuffix = databaseDirectorySuffix)

        // delete a necessary file store for all employees (entire directory)
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Employee.directoryName}").deleteRecursively()

        val ex = assertThrows(DatabaseCorruptedException::class.java) {
            DatabaseDiskPersistence(
                "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/", testLogger)
                .startWithDiskPersistence()
        }

        // expect it to fail while reading time entries and attempting to associate with (now missing) employee
        assertEquals("Unable to find an employee with the id of 2 while deserializing a time entry.  Employee set size: 0", ex.message)
    }
    
    /**
     * See [testPersistence_Read_CorruptedData_TimeEntries_BadData]
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_CorruptedData_Users_BadData() {
        val databaseDirectorySuffix = "testPersistence_Read_CorruptedData_Users_BadData"
        arrangeFullDatabaseWithDisk(skipRestarting = true, databaseDirectorySuffix = databaseDirectorySuffix)

        // create a corrupt user data file
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${User.directoryName}/99$databaseFileSuffix").writeText("BAD DATA HERE")

        val ex = assertThrows(DatabaseCorruptedException::class.java) {
            DatabaseDiskPersistence(
                "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/", testLogger)
                .startWithDiskPersistence()
        }
        assertEquals("Unable to deserialize this text from the users directory: BAD DATA HERE", ex.message)
    }
    
    /**
     * See [testPersistence_Read_CorruptedData_Employees_MissingFile]
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_CorruptedData_Users_MissingFile() {
        val databaseDirectorySuffix = "testPersistence_Read_CorruptedData_Users_MissingFile"
        arrangeFullDatabaseWithDisk(skipRestarting = true, databaseDirectorySuffix = databaseDirectorySuffix)

        // delete the file store representing the entire users set (directory in this case)
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${User.directoryName}").deleteRecursively()

        val ex = assertThrows(DatabaseCorruptedException::class.java) {
            DatabaseDiskPersistence(
                "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/", testLogger)
                .startWithDiskPersistence()
        }
        assertEquals("Unable to find a user with the id of 1.  User set size: 0", ex.message)
    }
    
    /**
     * See [testPersistence_Read_CorruptedData_TimeEntries_BadData]
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_CorruptedData_Projects_BadData() {
        val databaseDirectorySuffix = "testPersistence_Read_CorruptedData_Projects_BadData"
        arrangeFullDatabaseWithDisk(skipRestarting = true, databaseDirectorySuffix = databaseDirectorySuffix)
        val project = pmd.ProjectDataAccess().read { p -> p.single{ it.name == DEFAULT_PROJECT_NAME }}

        // corrupt the projects data file
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Project.directoryName}/${project.id.value}$databaseFileSuffix").writeText("BAD DATA HERE")

        val ex = assertThrows(DatabaseCorruptedException::class.java) {
            DatabaseDiskPersistence(
                "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/", testLogger)
                .startWithDiskPersistence()
        }
        assertEquals("Unable to deserialize this text from the projects directory: BAD DATA HERE", ex.message)
    }
    
    /**
     * See [testPersistence_Read_CorruptedData_Employees_MissingFile]
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_CorruptedData_Projects_MissingFile() {
        val databaseDirectorySuffix = "testPersistence_Read_CorruptedData_Projects_MissingFile"
        arrangeFullDatabaseWithDisk(skipRestarting = true, databaseDirectorySuffix = databaseDirectorySuffix)

        // delete a necessary file
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Project.directoryName}").deleteRecursively()

        val ex = assertThrows(DatabaseCorruptedException::class.java) {
            DatabaseDiskPersistence(
                "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/", testLogger)
                .startWithDiskPersistence()
        }
        assertEquals("Unable to find a project with the id of 1 while deserializing a time entry.  Project set size: 0", ex.message)
    }
    
    /**
     * See [testPersistence_Read_CorruptedData_TimeEntries_BadData]
     */
    @IntegrationTest(usesDirectory=true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testPersistence_Read_CorruptedData_Sessions_BadData() {
        val databaseDirectorySuffix = "testPersistence_Read_CorruptedData_Sessions_BadData"
        arrangeFullDatabaseWithDisk(skipRestarting = true, databaseDirectorySuffix = databaseDirectorySuffix)

        // create corrupt bogus time-entries data file
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Session.directoryName}/99$databaseFileSuffix").writeText("BAD DATA HERE")

        assertThrows(DatabaseCorruptedException::class.java) {
            DatabaseDiskPersistence(
                "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/", testLogger)
                .startWithDiskPersistence()
        }
    }

    enum class InvalidKeyHasSpace(private val keyString: String) : SerializationKeys {
        ID("id id");

        /**
         * This needs to be a method and not just a value of the class
         * so that we can have it meet an interface specification, so
         * that we can use it in generic code
         */
        override fun getKey() : String {
            return keyString
        }
    }

    /**
     * Serialization keys cannot contain spaces
     */
    @Test
    fun testSerializationDisallowsSpaces() {

        class Foo : IndexableSerializable() {

            override fun getIndex(): Int {
                // no need to implement
                return 0
            }

            override val dataMappings: Map<SerializationKeys, String>
                get() = mapOf(
                    InvalidKeyHasSpace.ID to "DOES_NOT_MATTER",
                )
        }

        val ex = assertThrows(IllegalStateException::class.java) { Foo().serialize() }
        assertEquals("Serialization keys must match this regex: [a-zA-Z]{1,10}.  Your key was: id id", ex.message)
    }


    enum class InvalidKeyTooLong(private val keyString: String) : SerializationKeys {
        ID("bcdefghijkl");

        /**
         * This needs to be a method and not just a value of the class
         * so that we can have it meet an interface specification, so
         * that we can use it in generic code
         */
        override fun getKey() : String {
            return keyString
        }
    }

    /**
     * Serialization keys cannot be longer than allowable
     */
    @Test
    fun testSerializationDisallowsTooLong() {

        class Foo : IndexableSerializable() {

            override fun getIndex(): Int {
                // no need to implement
                return 0
            }

            override val dataMappings: Map<SerializationKeys, String>
                get() = mapOf(
                    InvalidKeyTooLong.ID to "DOES_NOT_MATTER",
                )
        }

        val ex = assertThrows(IllegalStateException::class.java) { Foo().serialize() }
        assertEquals("Serialization keys must match this regex: [a-zA-Z]{1,10}.  Your key was: bcdefghijkl", ex.message)
    }

    enum class InvalidKeyTooShort(private val keyString: String) : SerializationKeys {
        ID("");

        /**
         * This needs to be a method and not just a value of the class
         * so that we can have it meet an interface specification, so
         * that we can use it in generic code
         */
        override fun getKey() : String {
            return keyString
        }
    }

    /**
     * Serialization keys must be at least one character
     */
    @Test
    fun testSerializationDisallowsTooShort() {

        class Foo : IndexableSerializable() {

            override fun getIndex(): Int {
                // no need to implement
                return 0
            }

            override val dataMappings: Map<SerializationKeys, String>
                get() = mapOf(
                    InvalidKeyTooShort.ID to "DOES_NOT_MATTER",
                )
        }

        val ex = assertThrows(IllegalStateException::class.java) { Foo().serialize() }
        assertEquals("Serialization keys must match this regex: [a-zA-Z]{1,10}.  Your key was: (BLANK)", ex.message)
    }

    enum class InvalidKeyNumbers(private val keyString: String) : SerializationKeys {
        ID("abcd123");

        /**
         * This needs to be a method and not just a value of the class
         * so that we can have it meet an interface specification, so
         * that we can use it in generic code
         */
        override fun getKey() : String {
            return keyString
        }
    }

    /**
     * Serialization keys cannot contain numbers
     */
    @Test
    fun testSerializationDisallowsNumbers() {

        class Foo : IndexableSerializable() {

            override fun getIndex(): Int {
                // no need to implement
                return 0
            }

            override val dataMappings: Map<SerializationKeys, String>
                get() = mapOf(
                    InvalidKeyNumbers.ID to "DOES_NOT_MATTER",
                )
        }

        val ex = assertThrows(IllegalStateException::class.java) { Foo().serialize() }
        assertEquals("Serialization keys must match this regex: [a-zA-Z]{1,10}.  Your key was: abcd123", ex.message)
    }

    enum class InvalidKeysNonUnique(private val keyString: String) : SerializationKeys {
        ID("id"),
        OTHER_ID("id");

        /**
         * This needs to be a method and not just a value of the class
         * so that we can have it meet an interface specification, so
         * that we can use it in generic code
         */
        override fun getKey() : String {
            return keyString
        }
    }

    /**
     * Serialization keys be duplicates.
     * two keys with the string "id" is disallowed.
     */
    @Test
    fun testSerializationDisallowsNonUnique() {

        class Foo : IndexableSerializable() {

            override fun getIndex(): Int {
                // no need to implement
                return 0
            }

            override val dataMappings: Map<SerializationKeys, String>
                get() = mapOf(
                    InvalidKeysNonUnique.ID to "DOES_NOT_MATTER",
                    InvalidKeysNonUnique.OTHER_ID to "DOES_NOT_MATTER",
                )
        }

        val ex = assertThrows(IllegalStateException::class.java) { Foo().serialize() }
        assertEquals("Serialization keys must be unique.  Here are your keys: [id, id]", ex.message)
    }

    @Test
    fun testSerialization_User() {
        val user = User(UserId(1), UserName("myname"), Hash("myhash"), Salt("mysalt"), EmployeeId(1))

        val result = user.serialize()

        assertEquals("""{ id: 1 , name: myname , hash: myhash , salt: mysalt , empId: 1 }""", result)

        val deserialized = User.Deserializer().deserialize(result)

        assertEquals(user, deserialized)
    }

    @Test
    fun testSerialization_UserWithMultilineText() {
        val user = User(
            UserId(1), UserName("myname"), Hash("myhash"), Salt("""mysalt
            |thisisalsotext""".trimMargin()), EmployeeId(1)
        )

        val result = user.serialize()

        assertEquals("""{ id: 1 , name: myname , hash: myhash , salt: mysalt%0Athisisalsotext , empId: 1 }""".trimMargin(), result)

        val deserialized = User.Deserializer().deserialize(result)

        assertEquals(user, deserialized)
    }

    @Test
    fun testSerialization_UserWithUnicodeText() {
        val user = User(UserId(1), UserName("myname"), Hash("myhash"), Salt("L¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿LÀÁÂÃÄÅÆÇÈÉÊË"), EmployeeId(1))

        val result = user.serialize()

        assertEquals("""{ id: 1 , name: myname , hash: myhash , salt: L%C2%A1%C2%A2%C2%A3%C2%A4%C2%A5%C2%A6%C2%A7%C2%A8%C2%A9%C2%AA%C2%AB%C2%AC%C2%AE%C2%AF%C2%B0%C2%B1%C2%B2%C2%B3%C2%B4%C2%B5%C2%B6%C2%B7%C2%B8%C2%B9%C2%BA%C2%BB%C2%BC%C2%BD%C2%BE%C2%BFL%C3%80%C3%81%C3%82%C3%83%C3%84%C3%85%C3%86%C3%87%C3%88%C3%89%C3%8A%C3%8B , empId: 1 }""", result)

        val deserialized = User.Deserializer().deserialize(result)

        assertEquals(user, deserialized)
    }


    @Test
    fun testSerialization_Employee() {
        val employee = Employee(EmployeeId(1), EmployeeName("myname"))

        val result = employee.serialize()

        assertEquals("""{ id: 1 , name: myname }""", result)

        val deserialized = Employee.Deserializer().deserialize(result)

        assertEquals(employee, deserialized)
    }

    @Test
    fun testSerialization_Employee_UnicodeAndMultiline() {
        val employee = Employee(EmployeeId(1), EmployeeName("\n\r\tHelloµ¶·¸¹º»¼½¾¿LÀÁÂÃÄÅÆ"))

        val result = employee.serialize()

        assertEquals("""{ id: 1 , name: %0A%0D%09Hello%C2%B5%C2%B6%C2%B7%C2%B8%C2%B9%C2%BA%C2%BB%C2%BC%C2%BD%C2%BE%C2%BFL%C3%80%C3%81%C3%82%C3%83%C3%84%C3%85%C3%86 }""", result)

        val deserialized = Employee.Deserializer().deserialize(result)

        assertEquals(employee, deserialized)
    }

    @Test
    fun testSerialization_Project() {
        val project = Project(ProjectId(1), ProjectName("myname"))

        val result = project.serialize()

        assertEquals("""{ id: 1 , name: myname }""", result)

        val deserialized = Project.Deserializer().deserialize(result)

        assertEquals(project, deserialized)
    }

    @Test
    fun testSerialization_Project_UnicodeAndMultiline() {
        val project = Project(ProjectId(1), ProjectName("\n\r\tHelloµ¶·¸¹º»¼½¾¿LÀÁÂÃÄÅÆ"))

        val result = project.serialize()

        assertEquals("""{ id: 1 , name: %0A%0D%09Hello%C2%B5%C2%B6%C2%B7%C2%B8%C2%B9%C2%BA%C2%BB%C2%BC%C2%BD%C2%BE%C2%BFL%C3%80%C3%81%C3%82%C3%83%C3%84%C3%85%C3%86 }""", result)

        val deserialized = Project.Deserializer().deserialize(result)

        assertEquals(project, deserialized)
    }

    @Test
    fun testSerialization_Session() {
        val session = Session(1, "abc123", DEFAULT_USER, DEFAULT_DATETIME)

        val result = session.serialize()

        assertEquals("""{ sid: 1 , s: abc123 , id: 1 , e: 1577836800 }""", result)

        val deserialized = Session.Deserializer(setOf(DEFAULT_USER)).deserialize(result)

        assertEquals(session, deserialized)
    }

    @Test
    fun testSerialization_SessionUnicodeAndMultiline() {
        val session = Session(1, "\n\rabc123½¾¿LÀÁ", DEFAULT_USER, DEFAULT_DATETIME)

        val result = session.serialize()

        assertEquals("""{ sid: 1 , s: %0A%0Dabc123%C2%BD%C2%BE%C2%BFL%C3%80%C3%81 , id: 1 , e: 1577836800 }""", result)

        val deserialized = Session.Deserializer(setOf(DEFAULT_USER)).deserialize(result)

        assertEquals(session, deserialized)
    }

    @Test
    fun testSerialization_TimeEntry() {
        val result = DEFAULT_TIME_ENTRY.serialize()

        assertEquals("""{ i: 1 , e: 1 , p: 1 , t: 60 , d: 18438 , dtl:  }""", result)

        val deserialized = TimeEntry.Deserializer(setOf(DEFAULT_EMPLOYEE), setOf(DEFAULT_PROJECT)).deserialize(result)

        assertEquals(DEFAULT_TIME_ENTRY, deserialized)
    }

    @Test
    fun testSerialization_SubmittedPeriod() {
        val result = DEFAULT_SUBMITTED_PERIOD.serialize()

        assertEquals("""{ id: 1 , eid: 1 , start: 2021-02-01 , end: 2021-02-15 }""", result)

        val deserialized = SubmittedPeriod.Deserializer(setOf(DEFAULT_EMPLOYEE)).deserialize(result)

        assertEquals(DEFAULT_SUBMITTED_PERIOD, deserialized)
    }

    /**
     * If the data to deserialize is just an empty string
     */
    @Test
    fun testSerialization_SubmittedPeriod_CorruptedText_EmptyString() {
        val ex = assertThrows(DatabaseCorruptedException::class.java) { SubmittedPeriod.Deserializer(setOf(DEFAULT_EMPLOYEE)).deserialize("") }
        assertEquals("Unable to deserialize this text as time entry data: ", ex.message)
    }

    @Test
    fun testSerialization_SubmittedPeriod_CorruptedText_MissingKey() {
        val ex = assertThrows(DatabaseCorruptedException::class.java) { SubmittedPeriod.Deserializer(setOf(DEFAULT_EMPLOYEE)).deserialize("""{ eid: 1 , start: 2021-02-01 , end: 2021-02-15 }""") }
        assertEquals("Unable to deserialize this text as time entry data: { eid: 1 , start: 2021-02-01 , end: 2021-02-15 }", ex.message)
    }

    @Test
    fun testSerialization_SubmittedPeriod_CorruptedText_BadType() {
        val ex = assertThrows(DatabaseCorruptedException::class.java) { SubmittedPeriod.Deserializer(setOf(DEFAULT_EMPLOYEE)).deserialize("""{ id: aaaaaa , eid: 1 , start: 2021-02-01 , end: 2021-02-15 }""") }
        assertEquals("Unable to deserialize this text as time entry data: { id: aaaaaa , eid: 1 , start: 2021-02-01 , end: 2021-02-15 }", ex.message)
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */


    /**
     * Helps us avoid a lot of repetition in the set of tests related to
     * corrupting data entries on disk and also reloading the database after
     * not having done certain things, like for example: not ever having logged in.
     */
    private fun arrangeFullDatabaseWithDisk(
        databaseDirectorySuffix : String = "",
        skipCreatingUser: Boolean = false,
        skipCreatingSession : Boolean = false,
        skipCreatingTimeEntries : Boolean = false,
        skipCreatingEmployees : Boolean = false,
        skipCreatingProjects : Boolean = false,
        skipRestarting : Boolean = false): PureMemoryDatabase? {

        val databaseDirectory = "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/"

        File(databaseDirectory).deleteRecursively()
        pmd = DatabaseDiskPersistence(databaseDirectory, testLogger).startWithDiskPersistence()
        val ap = AuthenticationPersistence(pmd, testLogger)
        val tep = TimeEntryPersistence(pmd, logger = testLogger)
        val newEmployee = if (! skipCreatingEmployees) {
            tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        } else {
            NO_EMPLOYEE
        }
        val newProject = if (! skipCreatingProjects) {
            tep.persistNewProject(DEFAULT_PROJECT_NAME)
        } else {
            NO_PROJECT
        }
        if (! skipCreatingUser) {
            val newUser = ap.createUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
            if (! skipCreatingSession) {
                ap.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)
            }
        }
        if (! skipCreatingTimeEntries) {
            tep.persistNewTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))
        }
        pmd.stop()

        return if (! skipRestarting) {
            DatabaseDiskPersistence(databaseDirectory, testLogger).startWithDiskPersistence()
        } else {
            null
        }
    }

    private fun recordManyTimeEntries(tep: ITimeEntryPersistence, numberOfEmployees: Int, numberOfProjects: Int, numberOfDays: Int) : List<Employee> {
        val lotsOfEmployees: List<String> = generateEmployeeNames()
        persistEmployeesToDatabase(tep, numberOfEmployees, lotsOfEmployees)
        val allEmployees: List<Employee> = readEmployeesFromDatabase(tep)
        persistProjectsToDatabase(tep, numberOfProjects)
        val allProjects: List<Project> = readProjectsFromDatabase(tep)
        enterTimeEntries(tep, numberOfDays, allEmployees, allProjects, numberOfEmployees)
        return allEmployees
    }

    private fun accumulateMinutesPerEachEmployee(tep: ITimeEntryPersistence, allEmployees: List<Employee>) {
        val (timeToAccumulate) = getTime {
            val minutesPerEmployeeTotal =
                    allEmployees.map { e -> tep.readTimeEntries(e).sumBy { te -> te.time.numberOfMinutes } }
                            .toList()
            testLogger.logAudit { "the time ${allEmployees[0].name.value} spent was ${minutesPerEmployeeTotal[0]}" }
            testLogger.logAudit { "the time ${allEmployees[1].name.value} spent was ${minutesPerEmployeeTotal[1]}" }
        }

        testLogger.logAudit { "It took $timeToAccumulate milliseconds to accumulate the minutes per employee" }
    }

    private fun readTimeEntriesForOneEmployee(tep: ITimeEntryPersistence, allEmployees: List<Employee>) {
        val (timeToGetAllTimeEntries) = getTime { tep.readTimeEntries(allEmployees[0]) }
        testLogger.logAudit { "It took $timeToGetAllTimeEntries milliseconds to get all the time entries for a employee" }
    }

    private fun enterTimeEntries(tep: ITimeEntryPersistence, numberOfDays: Int, allEmployees: List<Employee>, allProjects: List<Project>, numberOfEmployees: Int) {
        testLogger.turnOffAllLogging()
        val (timeToEnterAllTimeEntries) = getTime {
            for (day in 1..numberOfDays) {
                for (employee in allEmployees) {
                    tep.persistNewTimeEntry(TimeEntryPreDatabase(employee, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                    tep.persistNewTimeEntry(TimeEntryPreDatabase(employee, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                    tep.persistNewTimeEntry(TimeEntryPreDatabase(employee, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                    tep.persistNewTimeEntry(TimeEntryPreDatabase(employee, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                }
            }
        }
        testLogger.resetLogSettingsToDefault()
        testLogger.logAudit { "It took $timeToEnterAllTimeEntries milliseconds total to enter ${numberOfDays * 4} time entries for each of $numberOfEmployees employees" }
        testLogger.logAudit { "(That's a total of ${("%,d".format(numberOfDays * 4 * numberOfEmployees))} time entries)" }
    }

    private fun readProjectsFromDatabase(tep: ITimeEntryPersistence): List<Project> {
        val (timeToReadAllProjects, allProjects) = getTime { tep.getAllProjects()}
        testLogger.logAudit { "It took $timeToReadAllProjects milliseconds to read all the projects" }
        return allProjects
    }

    private fun persistProjectsToDatabase(tep: ITimeEntryPersistence, numberOfProjects: Int) {
        testLogger.turnOffAllLogging()
        val (timeToCreateProjects) =
                getTime { (1..numberOfProjects).forEach { i -> tep.persistNewProject(ProjectName("project$i")) } }
        testLogger.resetLogSettingsToDefault()
        testLogger.logAudit { "It took $timeToCreateProjects milliseconds to create $numberOfProjects projects" }
    }

    private fun readEmployeesFromDatabase(tep: ITimeEntryPersistence): List<Employee> {
        val (timeToReadAllEmployees, allEmployees) = getTime {
            tep.getAllEmployees()
        }
        testLogger.logAudit { "It took $timeToReadAllEmployees milliseconds to read all the employees" }
        return allEmployees
    }

    private fun persistEmployeesToDatabase(tep: ITimeEntryPersistence, numberOfEmployees: Int, lotsOfEmployees: List<String>) {
        testLogger.turnOffAllLogging()
        val (timeToEnterEmployees) = getTime {
            for (i in 1..numberOfEmployees) {
                tep.persistNewEmployee(EmployeeName(lotsOfEmployees[i]))
            }
        }
        testLogger.resetLogSettingsToDefault()
        testLogger.logAudit { "It took $timeToEnterEmployees milliseconds to enter $numberOfEmployees employees" }
    }

    private fun generateEmployeeNames(): List<String> {
        val (timeToMakeEmployeenames, lotsOfEmployees) = getTime {
            listOf(
                    "Arlen", "Hedwig", "Allix", "Tandi", "Silvia", "Catherine", "Mavis", "Hally", "Renate", "Anastasia", "Christy", "Nora", "Molly", "Nelli", "Daphna", "Chloette", "TEirtza", "Nannie", "Melinda", "Tyne", "Belva", "Pam", "Rebekkah", "Elayne", "Dianne", "Christina", "Jeanne", "Norry", "Reina", "Erminia", "Eadie", "Valina", "Gayle", "Wylma", "Annette", "Annmaria", "Fayina", "Dita", "Sibella", "Alis", "Georgena", "Luciana", "Sidonnie", "Dina", "Ferdinande", "Coletta", "Esma", "Saidee", "Hannah", "Colette", "Anitra", "Grissel", "Caritta", "Ann", "Rhodia", "Meta", "Bride", "Dalenna", "Rozina", "Ottilie", "Eliza", "Gerda", "Anthia", "Kathryn", "Lilian", "Jeannie", "Nichole", "Marylinda", "Angelica", "Margie", "Ruthie", "Augustina", "Netta", "Fleur", "Mahala", "Cosette", "Zsa Zsa", "Karry", "Tabitha", "Andriana", "Fey", "Hedy", "Saudra", "Geneva", "Lacey", "Fawnia", "Ertha", "Bernie", "Natty", "Joyan", "Teddie", "Hephzibah", "Vonni", "Ambur", "Lyndsie", "Anna", "Minnaminnie", "Andy", "Brina", "Pamella", "Trista", "Antonetta", "Kerrin", "Crysta", "Kira", "Gypsy", "Candy", "Ree", "Sharai", "Mariana", "Eleni", "Yetty", "Maisie", "Deborah", "Doretta", "Juliette", "Esta", "Amandi", "Anallise", "Indira", "Aura", "Melodee", "Desiri", "Jacquenetta", "Joell", "Delcine", "Justine", "Theresita", "Belia", "Mallory", "Antonie", "Jobi", "Katalin", "Kelli", "Ester", "Katey", "Gianna", "Berry", "Sidonia", "Roseanne", "Cherida", "Beatriz", "Eartha", "Robina", "Florri", "Vitoria", "Debera", "Jeanette", "Almire", "Saree", "Liana", "Ruth", "Renell", "Katinka", "Anya", "Gwyn", "Kaycee", "Rori", "Rianon", "Joann", "Zorana", "Hermia", "Gwenni", "Poppy", "Dedie", "Cloe", "Kirsti", "Krysta", "Clarinda", "Enid", "Katina", "Ralina", "Meryl", "Andie", "Orella", "Alexia", "Clarey", "Iris", "Chris", "Devin", "Kally", "Vernice", "Noelyn", "Stephana", "Catina", "Faydra", "Fionna", "Nola", "Courtnay", "Vera", "Meriel", "Eleonora", "Clare", "Marsha", "Marita", "Concettina", "Kristien", "Celina", "Maryl", "Codee", "Lorraine", "Lauraine", "Sephira", "Kym", "Odette", "Ranee", "Margaux", "Debra", "Corenda", "Mariejeanne", "Georgeanne", "Laural", "Fredelia", "Dulcine", "Tess", "Tina", "Adaline", "Melisandra", "Lita", "Nettie", "Lesley", "Clea", "Marysa", "Arleyne", "Meade", "Ella", "Theodora", "Morgan", "Carena", "Camille", "Janene", "Anett", "Camellia", "Guglielma", "Evvy", "Shayna", "Karilynn", "Ingeberg", "Maudie", "Colene", "Kelcy", "Blythe", "Lacy", "Cesya", "Bobbe", "Maggi", "Darline", "Almira", "Constantia", "Helaina", "Merrili", "Maxine", "Linea", "Marley", "Timmie", "Devon", "Mair", "Thomasine", "Sherry", "Gilli", "Ursa", "Marlena", "Cloris", "Vale", "Alexandra", "Angel", "Alice", "Ulrica", "Britteny", "Annie", "Juliane", "Candida", "Jennie", "Susanne", "Robenia", "Benny", "Cassy", "Denyse", "Jackquelin", "Lorelle", "Lenore", "Sheryl", "Marice", "Clarissa", "Kippy", "Cristen", "Hanni", "Marne", "Melody", "Shane", "Kalli", "Deane", "Kaila", "Faye", "Noella", "Penni", "Sophia", "Marilin", "Cori", "Clair", "Morna", "Lynn", "Rozelle", "Berta", "Bamby", "Janifer", "Doro", "Beryle", "Pammy", "Paige", "Juanita", "Ellene", "Kora", "Kerrie", "Perrine", "Dorena", "Mady", "Dorian", "Lucine", "Jill", "Octavia", "Sande", "Talyah", "Rafaelia", "Doris", "Patti", "Mora", "Marja", "Rivi", "Drucill", "Marina", "Rennie", "Annabell", "Xylia", "Zorina", "Ashil", "Becka", "Blithe", "Lenora", "Kattie", "Analise", "Jasmin", "Minetta", "Deeanne", "Sharity", "Merci", "Julissa", "Nicoli", "Nevsa", "Friederike", "Caroljean", "Catlee", "Charil", "Dara", "Kristy", "Ag", "Andriette", "Kati", "Jackqueline", "Letti", "Allys", "Carlee", "Frannie", "Philis", "Aili", "Else", "Diane", "Tobey", "Tildie", "Merrilee", "Pearle", "Christan", "Dominique", "Rosemaria", "Bunnie", "Tedi", "Elinor", "Aeriell", "Karissa", "Darya", "Tonye", "Alina", "Nalani", "Marcela", "Anabelle", "Layne", "Dorice", "Aleda", "Anette", "Arliene", "Rosemarie", "Pru", "Tiffani", "Addi", "Roda", "Shandra", "Wendeline", "Karoline", "Ciel", "Ania"
            )
            // if you want to make a lot more names, uncomment below
            // lotsOfEmployees = (1..10).flatMap { n -> employeenames.map { u -> "$u$n" } }.toList()
        }
        testLogger.logAudit { "It took $timeToMakeEmployeenames milliseconds to create ${lotsOfEmployees.size} employeenames" }
        return lotsOfEmployees
    }

}