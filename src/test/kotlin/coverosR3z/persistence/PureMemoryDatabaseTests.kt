package coverosR3z.persistence

import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.system.config.CURRENT_DATABASE_VERSION
import coverosR3z.system.misc.*
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.persistence.types.ChangeTrackingSet
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializationKeys
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.databaseFileSuffix
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.persistence.utility.PureMemoryDatabase.Companion.createEmptyDatabase
import coverosR3z.system.config.types.SystemConfiguration
import coverosR3z.timerecording.types.*
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.io.File
import java.lang.IllegalStateException

class PureMemoryDatabaseTests {

    private lateinit var pmd: PureMemoryDatabase

    @Before
    fun init() {
        pmd = createEmptyDatabase()
    }

    /**
     * I wish to make an exact copy of the PMD in completely new memory locations
     */
    @Test
    fun testShouldBePossibleToCopy_different() {
        val originalPmd = pmd.copy()
        pmd.dataAccess<Employee>(Employee.directoryName).actOn { it.add(DEFAULT_EMPLOYEE) }
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
        val (pmd, readPmd) = arrangeFullDatabaseWithDisk(databaseDirectorySuffix = "testPersistence_Read_HappyPath")

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
        val (pmd, readPmd) = arrangeFullDatabaseWithDisk(skipCreatingUser = true, databaseDirectorySuffix = "testPersistence_Read_MissingUsers")

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
        val (pmd, readPmd) = arrangeFullDatabaseWithDisk(skipCreatingSession = true, databaseDirectorySuffix = "testPersistence_Read_MissingSessions")

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
        val (pmd, readPmd) = arrangeFullDatabaseWithDisk(skipCreatingTimeEntries = true, databaseDirectorySuffix = databaseDirectorySuffix)

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
        val (pmd, readPmd) = arrangeFullDatabaseWithDisk(skipCreatingTimeEntries = true, databaseDirectorySuffix = "testPersistence_Read_MissingTimeEntries")

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
        val (pmd, readPmd) = arrangeFullDatabaseWithDisk(skipCreatingTimeEntries = true, databaseDirectorySuffix = "testPersistence_Read_MissingEmployees")

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
        val (pmd, readPmd) = arrangeFullDatabaseWithDisk(skipCreatingTimeEntries = true, databaseDirectorySuffix = databaseDirectorySuffix)

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
        val (pmd, readPmd) = arrangeFullDatabaseWithDisk(skipCreatingProjects = true, skipCreatingTimeEntries = true, databaseDirectorySuffix = "testPersistence_Read_MissingProjects")

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
        val (pmd, readPmd) = arrangeFullDatabaseWithDisk(skipCreatingTimeEntries = true, databaseDirectorySuffix = databaseDirectorySuffix)

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
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${TimeEntry.directoryName}/2$databaseFileSuffix").writeText(
            BAD_DATA_HERE
        )

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
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Employee.directoryName}/27$databaseFileSuffix").writeText(BAD_DATA_HERE)
        
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
        assertEquals("Unable to find an employee with the id of 1 while deserializing a user.  Employee set size: 0", ex.message)
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
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${User.directoryName}/99$databaseFileSuffix").writeText(BAD_DATA_HERE)

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
        val project = pmd.dataAccess<Project>(Project.directoryName).read { p -> p.single{ it.name == DEFAULT_PROJECT_NAME }}

        // corrupt the projects data file
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Project.directoryName}/${project.id.value}$databaseFileSuffix").writeText(BAD_DATA_HERE)

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
        File("$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/$CURRENT_DATABASE_VERSION/${Session.directoryName}/99$databaseFileSuffix").writeText(BAD_DATA_HERE)

        assertThrows(DatabaseCorruptedException::class.java) {
            DatabaseDiskPersistence(
                "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/", testLogger)
                .startWithDiskPersistence()
        }
    }

    enum class InvalidKeyHasSpace(override val keyString: String) : SerializationKeys {
        ID("id id");
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


    enum class InvalidKeyTooLong(override val keyString: String) : SerializationKeys {
        ID("bcdefghijkl");
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

    enum class InvalidKeyTooShort(override val keyString: String) : SerializationKeys {
        ID("");
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

    enum class InvalidKeyNumbers(override val keyString: String) : SerializationKeys {
        ID("abcd123");
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

    enum class InvalidKeysNonUnique(override val keyString: String) : SerializationKeys {
        ID("id"),
        OTHER_ID("id");
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
        val result = DEFAULT_USER.serialize()

        assertEquals("""{ id: 1 , name: DefaultUser , hash: 4dc91e9a80320c901f51ccf7166d646c , salt: 12345 , empId: 1 , role: REGULAR }""", result)

        val deserialized = User.Deserializer(setOf(DEFAULT_EMPLOYEE)).deserialize(result)

        assertEquals(DEFAULT_USER, deserialized)
    }

    @Test
    fun testSerialization_UserWithMultilineText() {
        val user = User(
            UserId(1), UserName("myname"), Hash("myhash"), Salt("""mysalt
            |thisisalsotext""".trimMargin()), DEFAULT_EMPLOYEE, Role.REGULAR
        )

        val result = user.serialize()

        assertEquals("""{ id: 1 , name: myname , hash: myhash , salt: mysalt%0Athisisalsotext , empId: 1 , role: REGULAR }""".trimMargin(), result)

        val deserialized = User.Deserializer(setOf(DEFAULT_EMPLOYEE)).deserialize(result)

        assertEquals(user, deserialized)
    }

    @Test
    fun testSerialization_UserWithUnicodeText() {
        val user = User(UserId(1), UserName("myname"), Hash("myhash"), Salt("L¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿LÀÁÂÃÄÅÆÇÈÉÊË"), DEFAULT_EMPLOYEE, Role.REGULAR)

        val result = user.serialize()

        assertEquals("""{ id: 1 , name: myname , hash: myhash , salt: L%C2%A1%C2%A2%C2%A3%C2%A4%C2%A5%C2%A6%C2%A7%C2%A8%C2%A9%C2%AA%C2%AB%C2%AC%C2%AE%C2%AF%C2%B0%C2%B1%C2%B2%C2%B3%C2%B4%C2%B5%C2%B6%C2%B7%C2%B8%C2%B9%C2%BA%C2%BB%C2%BC%C2%BD%C2%BE%C2%BFL%C3%80%C3%81%C3%82%C3%83%C3%84%C3%85%C3%86%C3%87%C3%88%C3%89%C3%8A%C3%8B , empId: 1 , role: REGULAR }""", result)

        val deserialized = User.Deserializer(setOf(DEFAULT_EMPLOYEE)).deserialize(result)

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

        assertEquals("""{ id: 1 , eid: 1 , start: 2021-02-01 , end: 2021-02-15 , appr: UNAPPROVED }""", result)

        val deserialized = SubmittedPeriod.Deserializer(setOf(DEFAULT_EMPLOYEE)).deserialize(result)

        assertEquals(DEFAULT_SUBMITTED_PERIOD, deserialized)
    }

    /**
     * If the data to deserialize is just an empty string
     */
    @Test
    fun testSerialization_SubmittedPeriod_CorruptedText_EmptyString() {
        val ex = assertThrows(DatabaseCorruptedException::class.java) { SubmittedPeriod.Deserializer(setOf(DEFAULT_EMPLOYEE)).deserialize("") }
        assertEquals("Unable to deserialize this text as submission data: ", ex.message)
    }

    @Test
    fun testSerialization_SubmittedPeriod_CorruptedText_MissingKey() {
        val ex = assertThrows(DatabaseCorruptedException::class.java) { SubmittedPeriod.Deserializer(setOf(DEFAULT_EMPLOYEE)).deserialize("""{ eid: 1 , start: 2021-02-01 , end: 2021-02-15 }""") }
        assertEquals("Unable to deserialize this text as submission data: { eid: 1 , start: 2021-02-01 , end: 2021-02-15 }", ex.message)
    }

    @Test
    fun testSerialization_SubmittedPeriod_CorruptedText_BadType() {
        val ex = assertThrows(DatabaseCorruptedException::class.java) { SubmittedPeriod.Deserializer(setOf(DEFAULT_EMPLOYEE)).deserialize("""{ id: aaaaaa , eid: 1 , start: 2021-02-01 , end: 2021-02-15 }""") }
        assertEquals("Unable to deserialize this text as submission data: { id: aaaaaa , eid: 1 , start: 2021-02-01 , end: 2021-02-15 }", ex.message)
    }

    /**
     * If we submit a time period, then restart the database, it
     * should still be there.
     */
    @Test
    fun testRestoreSubmittedPeriods() {
        val (before, after) = arrangeFullDatabaseWithDisk(databaseDirectorySuffix = "testRestoreSubmittedPeriods")

        assertEquals(before, after)
    }

    @Test
    fun testUsingMapOfData() {
        val d = PureMemoryDatabase(data = mapOf(Employee.directoryName to ChangeTrackingSet<Employee>()))
        val dataAccess = d.dataAccess<Employee>(Employee.directoryName)
        val result = dataAccess.read { employees -> employees.filter { it.name == DEFAULT_EMPLOYEE.name} }
        println(result)
        dataAccess.actOn { employees -> employees.add(DEFAULT_EMPLOYEE) }
        val result2 = dataAccess.read { employees -> employees.filter { it.name == DEFAULT_EMPLOYEE.name} }
        println(result2)
    }

    @Test
    fun testSerialization_Invitation() {
        val result = DEFAULT_INVITATION.serialize()

        assertEquals("""{ id: 1 , c: abc123 , eid: 1 , d: 1577836800 }""", result)

        val deserialized = Invitation.Deserializer(setOf(DEFAULT_EMPLOYEE)).deserialize(result)

        assertEquals(DEFAULT_INVITATION, deserialized)
    }

    @Test
    fun testSerialization_SystemConfiguration() {
        val result = DEFAULT_CONFIGURATION.serialize()

        assertEquals("{ id: 1 , la: true , lw: true , ld: true , lt: true }", result)

        val deserialized = SystemConfiguration.Deserializer().deserialize(result)

        assertEquals(DEFAULT_CONFIGURATION, deserialized)
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
     *
     * @return first returned value is the database *before* restarting, second is
     *         the one *after* restarting
     */
    private fun arrangeFullDatabaseWithDisk(
        databaseDirectorySuffix : String = "",
        skipCreatingUser: Boolean = false,
        skipCreatingSession : Boolean = false,
        skipCreatingTimeEntries : Boolean = false,
        skipUpdatingTimeEntry : Boolean = false,
        skipCreatingEmployees : Boolean = false,
        skipCreatingProjects : Boolean = false,
        skipCreatingSubmissions : Boolean = false,
        skipRestarting : Boolean = false): Pair<PureMemoryDatabase, PureMemoryDatabase?> {

        val databaseDirectory = "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/"

        File(databaseDirectory).deleteRecursively()
        pmd = DatabaseDiskPersistence(databaseDirectory, testLogger).startWithDiskPersistence()
        val au = AuthenticationUtilities(pmd, testLogger, CurrentUser(SYSTEM_USER))
        val tru = TimeRecordingUtilities(pmd, logger = testLogger, cu = CurrentUser(DEFAULT_ADMIN_USER))
        val newEmployee = if (! skipCreatingEmployees) {
            tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        } else {
            NO_EMPLOYEE
        }
        val newProject = if (! skipCreatingProjects) {
            tru.createProject(DEFAULT_PROJECT_NAME)
        } else {
            NO_PROJECT
        }
        if (! skipCreatingUser) {
            val (_,newUser) = au.registerWithEmployee(DEFAULT_USER.name, DEFAULT_PASSWORD, newEmployee)
            if (! skipCreatingSession) {
                au.createNewSession(newUser, time = DEFAULT_DATETIME, rand = {DEFAULT_SESSION_TOKEN})
            }
        }
        if (! skipCreatingTimeEntries) {
            val (_, newEntry) = tru.createTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))
            if (! skipUpdatingTimeEntry) {
                tru.changeEntry(newEntry!!.copy(time = Time(4 * 60)))
            }
        }

        if(! skipCreatingSubmissions) {
            tru.submitTimePeriod(TimePeriod.getTimePeriodForDate(DEFAULT_DATE))
        }
        pmd.stop()

        return if (! skipRestarting) {
            Pair(pmd, DatabaseDiskPersistence(databaseDirectory, testLogger).startWithDiskPersistence())
        } else {
            Pair(pmd, null)
        }
    }

    companion object {
        const val BAD_DATA_HERE = "BAD DATA HERE"
    }

}