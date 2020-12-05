package coverosR3z.persistence

import coverosR3z.*
import coverosR3z.domainobjects.*
import coverosR3z.exceptions.DatabaseCorruptedException
import coverosR3z.exceptions.EmployeeNotRegisteredException
import coverosR3z.logging.logAudit
import coverosR3z.misc.getTime
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.lang.IllegalArgumentException
import kotlin.concurrent.thread

class PureMemoryDatabaseTests {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            // wipe out the database
            File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        }

    }

    private lateinit var pmd: PureMemoryDatabase

    @Before
    fun init() {
        pmd = PureMemoryDatabase()
    }

    @Test
    fun `should be able to add a new project`() {
        pmd.addNewProject(DEFAULT_PROJECT_NAME)

        val project = pmd.getProjectById(DEFAULT_PROJECT.id)

        assertEquals(ProjectId(1), project.id)
    }

    @Test
    fun `should be able to add a new employee`() {
        pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)

        val employee = pmd.getEmployeeById(DEFAULT_EMPLOYEE.id)

        assertEquals(1, employee.id.value)
    }

    @Test
    fun `should be able to add a new time entry`() {
        pmd.addTimeEntry(TimeEntryPreDatabase(DEFAULT_EMPLOYEE, DEFAULT_PROJECT, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020))

        val timeEntries = pmd.getAllTimeEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020).first()

        assertEquals(1, timeEntries.id)
        assertEquals(DEFAULT_EMPLOYEE, timeEntries.employee)
        assertEquals(DEFAULT_PROJECT, timeEntries.project)
        assertEquals(DEFAULT_TIME, timeEntries.time)
        assertEquals(A_RANDOM_DAY_IN_JUNE_2020, timeEntries.date)
    }

    @Test
    fun `PERFORMANCE a firm should get responses from the database quite quickly`() {
        val numberOfEmployees = 30
        val numberOfProjects = 30
        val numberOfDays = 31

        val allEmployees = recordManyTimeEntries(numberOfEmployees, numberOfProjects, numberOfDays)

        val (totalTime) = getTime {
            readTimeEntriesForOneEmployee(allEmployees)
            accumulateMinutesPerEachEmployee(allEmployees)
        }

        logAudit("It took a total of $totalTime milliseconds for this code")
        assertTrue(totalTime < 100)
    }

    @Test
    fun `should be able to get the minutes on a certain date`() {
        pmd.addNewEmployee(DEFAULT_EMPLOYEE.name)
        pmd.addTimeEntry(TimeEntryPreDatabase(DEFAULT_EMPLOYEE, DEFAULT_PROJECT, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020))

        val minutes = pmd.getMinutesRecordedOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals(DEFAULT_TIME, minutes)
    }

    @Test
    fun `should not be able to get minutes recorded for an unregistered employee`() {
        assertThrows(EmployeeNotRegisteredException::class.java) {pmd.getMinutesRecordedOnDate(Employee(EmployeeId(7), EmployeeName("Harold")), A_RANDOM_DAY_IN_JUNE_2020)}
    }

    /**
     * If I ask the database for all the time entries for a particular employee on
     * a date and there aren't any, I should get back an empty list, not a null.
     */
    @Test
    fun testShouldReturnEmptyListIfNoEntries() {
        val result = pmd.getAllTimeEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020)
        assertEquals(emptySet<TimeEntry>() , result)
    }

    /**
     * If a user successfully authenticates, we should create a session entry,
     */
    @Test
    fun testShouldAddSession() {
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)
        assertEquals(DEFAULT_USER, pmd.getUserBySessionToken(DEFAULT_SESSION_TOKEN))
    }

    /**
     * If we try to add a session for a user when one already exists, throw exception
     */
    @Test
    fun testShouldAddSession_Duplicate() {
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)
        val ex = assertThrows(IllegalArgumentException::class.java) {pmd.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)}
        assertEquals("There must not already exist a session for (${DEFAULT_USER.name}) if we are to create one", ex.message)
    }

    /**
     * When a user is no longer authenticated, we enact that
     * by removing their entry from the sessions.
     */
    @Test
    fun testShouldRemoveSession() {
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)
        assertEquals(DEFAULT_USER, pmd.getUserBySessionToken(DEFAULT_SESSION_TOKEN))
        pmd.removeSessionByToken(DEFAULT_SESSION_TOKEN)
        assertEquals(NO_USER, pmd.getUserBySessionToken(DEFAULT_SESSION_TOKEN))
    }

    /**
     * If we try to remove a session but it doesn't exist, throw an exception
     */
    @Test
    fun testShouldComplainIfTryingToRemoveNonexistentSession() {
        val ex = assertThrows(java.lang.IllegalStateException::class.java) {pmd.removeSessionByToken(DEFAULT_SESSION_TOKEN)}
        assertEquals("There must exist a session in the database for (${DEFAULT_SESSION_TOKEN}) in order to delete it", ex.message)
    }

    /**
     * I wish to make an exact copy of the PMD in completely new memory locations
     */
    @Test
    fun testShouldBePossibleToCopy_different() {
        val originalPmd = pmd.copy()
        pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
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
    @Test
    fun testShouldWriteAndReadToDisk_PERFORMANCE() {
        val numberOfEmployees = 3
        val numberOfProjects = 5
        val numberOfDays = 2
        val maxMillisecondsAllowed = 200
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        File(DEFAULT_DB_DIRECTORY).mkdirs()
        val (totalTime) = getTime {
            recordManyTimeEntries(numberOfEmployees, numberOfProjects, numberOfDays)

            val (timeToReadText, deserializedPmd) = getTime {PureMemoryDatabase.deserializeFromDisk(DEFAULT_DB_DIRECTORY)}
            logAudit("it took $timeToReadText milliseconds to deserialize from disk")

            val (timeToAssert) = getTime {assertEquals(pmd, deserializedPmd)}
            logAudit("it took $timeToAssert milliseconds to assert the databases were equal")
        }

        logAudit("Total time taken for serialization / deserialzation was $totalTime milliseconds")

        assertTrue("totaltime was supposed to take $maxMillisecondsAllowed.  took $totalTime", totalTime < maxMillisecondsAllowed)
    }

    /**
     * How long does it actually take to serialize and deserialize?
     * Not very long at all.
     */
    @Test
    fun testSerializingTimeEntries_PERFORMANCE() {
        val numTimeEntries = 1000
        val projects = mutableSetOf(DEFAULT_PROJECT)
        val employees = mutableSetOf(DEFAULT_EMPLOYEE)
        val timeEntries: MutableSet<TimeEntry> = prepareSomeRandomTimeEntries(numTimeEntries, projects, employees)

        val (timeToSerialize, serializedTimeEntries) = getTime{PureMemoryDatabase.serializeTimeEntries(timeEntries)}
        val (timeToDeserialize, _) = getTime{PureMemoryDatabase.deserializeTimeEntries(serializedTimeEntries, "", employees, projects)}

        logAudit("Time to serialize $numTimeEntries time entries was $timeToSerialize milliseconds")
        logAudit("Time to deserialize $numTimeEntries time entries was $timeToDeserialize milliseconds")

        assertTrue(timeToSerialize < 150)
        assertTrue(timeToDeserialize < 150)
    }

    /**
     * This is to test that it is possible to corrupt the data when
     * multiple threads are changing it
     *
     * The idea behind this test is that if we aren't handling
     * the threading cleanly, we won't get one hundred employees
     * added, it will be some smaller number.
     *
     * When I added the lock, this caused us to have a result
     * of the proper number of new employees
     *
     * To see this fail, just remove the locking mechanism from the
     * method at [PureMemoryDatabase.addNewEmployee], but you
     * might need to run it a time or two to see it fail.
     */
    @Test
    fun testCorruptingEmployeeDataWithMultiThreading() {
        val pmd = PureMemoryDatabase()
        val listOfThreads = mutableListOf<Thread>()
        val numberNewEmployeesAdded = 20
        repeat(numberNewEmployeesAdded) { // each thread calls the add a single time
            listOfThreads.add(thread {
                pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
            })
        }
        // wait for all those threads
        listOfThreads.forEach{it.join()}
        assertEquals(numberNewEmployeesAdded, pmd.getAllEmployees().size)
    }

    /**
     * See [testCorruptingEmployeeDataWithMultiThreading]
     */
    @Test
    fun testCorruptingUserDataWithMultiThreading() {
        val pmd = PureMemoryDatabase()
        val listOfThreads = mutableListOf<Thread>()
        val numberNewUsersAdded = 20
        repeat(numberNewUsersAdded) { // each thread calls the add a single time
            listOfThreads.add(thread {
                pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, DEFAULT_EMPLOYEE.id)
            })
        }
        // wait for all those threads
        listOfThreads.forEach{it.join()}
        assertEquals(numberNewUsersAdded, pmd.getAllUsers().size)
    }

    /**
     * See [testCorruptingEmployeeDataWithMultiThreading]
     */
    @Test
    fun testCorruptingProjectDataWithMultiThreading() {
        val pmd = PureMemoryDatabase()
        val listOfThreads = mutableListOf<Thread>()
        val numberNewProjectsAdded = 20
        repeat(numberNewProjectsAdded) { // each thread calls the add a single time
            listOfThreads.add(thread {
                pmd.addNewProject(DEFAULT_PROJECT_NAME)
            })
        }
        // wait for all those threads
        listOfThreads.forEach{it.join()}
        assertEquals(numberNewProjectsAdded, pmd.getAllProjects().size)
    }


    /**
     * See [testCorruptingEmployeeDataWithMultiThreading]
     */
    @Test
    fun testCorruptingSessionDataWithMultiThreading() {
        val pmd = PureMemoryDatabase()
        val listOfThreads = mutableListOf<Thread>()
        val numberNewSessionsAdded = 20
        for(i in 1..numberNewSessionsAdded) { // each thread calls the add a single time
            listOfThreads.add(thread {
                pmd.addNewSession(DEFAULT_SESSION_TOKEN+i, DEFAULT_USER, DEFAULT_DATETIME)
            })
        }
        // wait for all those threads
        listOfThreads.forEach{it.join()}
        assertEquals(numberNewSessionsAdded, pmd.getAllSessions().size)

        val listOfThreads2 = mutableListOf<Thread>()
        for(i in 1..numberNewSessionsAdded) { // each thread calls the add a single time
            listOfThreads2.add(thread {
                pmd.removeSessionByToken(DEFAULT_SESSION_TOKEN+i)
            })
        }
        // wait for all those threads
        listOfThreads2.forEach{it.join()}
        assertEquals(0, pmd.getAllSessions().size)
    }

    /**
     * Time entry recording is fairly involved, we have to lock
     * a lot.  See [testCorruptingEmployeeDataWithMultiThreading]
     */
    @Test
    fun testCorruptingTimeEntryDataWithMultiThreading() {
        val pmd = PureMemoryDatabase()
        val listOfThreads = mutableListOf<Thread>()
        val numberTimeEntriesAdded = 20
        repeat(numberTimeEntriesAdded) { // each thread calls the add a single time
            listOfThreads.add(thread {
                pmd.addTimeEntry(createTimeEntryPreDatabase())
            })
        }
        // wait for all those threads
        listOfThreads.forEach{it.join()}
        assertEquals(numberTimeEntriesAdded, pmd.getAllTimeEntriesForEmployee(DEFAULT_EMPLOYEE).size)
    }

    /**
     * Assuming we have valid data stored on disk, do we read
     * it back properly?
     */
    @Test
    fun testPersistence_Read_HappyPath() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)
        pmd.addTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))

        val readPmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)

        assertEquals(pmd, readPmd)
    }

    /**
     * These tests capture what happens when a file doesn't exist in the directory
     * because no entries have been, not because it's become corrupted.
     */
    @Test
    fun testPersistence_Read_MissingTimeEntries() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)

        val readPmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)

        assertEquals(pmd, readPmd)
    }

    /**
     * These tests capture what happens when a file doesn't exist in the directory
     * because no entries have been, not because it's become corrupted.
     *
     * here there are no employees, so there cannot be any time entries
     */
    @Test
    fun testPersistence_Read_MissingEmployees() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, null)
        pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)

        val readPmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)

        assertEquals(pmd, readPmd)
    }

    /**
     * These tests capture what happens when a file doesn't exist in the directory
     * because no entries have been, not because it's become corrupted.
     *
     * Here, there are no projects, which also means there cannot be time entries
     */
    @Test
    fun testPersistence_Read_MissingProjects() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)

        val readPmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)

        assertEquals(pmd, readPmd)
    }

    /**
     * These tests capture what happens when a file doesn't exist in the directory
     * because no entries have been, not because it's become corrupted.
     *
     * Here there are no users, so there cannot be any sessions either
     */
    @Test
    fun testPersistence_Read_MissingUsers() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))

        val readPmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)

        assertEquals(pmd, readPmd)
    }

    /**
     * These tests capture what happens when a file doesn't exist in the directory
     * because no entries have been, not because it's become corrupted.
     *
     * Here there are no sessions, which might mean no one was logged in
     */
    @Test
    fun testPersistence_Read_MissingSessions() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))

        val readPmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)

        assertEquals(pmd, readPmd)
    }

    /**
     * If we're starting and the directory for the database exists but no
     * files or subdirectories in it.  everything should 
     * still work as before
     */
    @Test
    fun testPersistence_Read_MissingAllFilesButDirectoryExists() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        File(DEFAULT_DB_DIRECTORY).mkdirs()
        PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        assertTrue("a new database will store its version", File(DEFAULT_DB_DIRECTORY + "version.txt").exists())
    }

    /**
     * If we are starting and try to read and there's no 
     * database directory.  Just make one.
     */
    @Test
    fun testPersistence_Read_MissingDbDirectory() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        assertTrue("a new database will store its version", File(DEFAULT_DB_DIRECTORY + "version.txt").exists())
    }

    /**
     * What if some of the data in the time-entries file is
     * corrupted? I think the most appropriate
     * response is to halt and yell for help, because at that point all
     * bets are off, we won't have enough information to properly recover.
     */
    @Test
    fun testPersistence_Read_CorruptedData_TimeEntries_BadData() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)
        pmd.addTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))
        
        // corrupt the time-entries data file
        File("$DEFAULT_DB_DIRECTORY/timeentries/2/2020_6.json").writeText("BAD DATA HERE")

        val ex = assertThrows(DatabaseCorruptedException::class.java) {PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)}
        assertEquals("Could not deserialize time entry file 2020_6.json.  deserializer exception message: Unexpected JSON token at offset 0: Expected '[, kind: LIST' - JSON input: BAD DATA HERE", ex.message)
    }

    /**
     * See [testPersistence_Read_CorruptedData_TimeEntries_BadData]
     */
    @Test
    fun testPersistence_Read_CorruptedData_Employees_BadData() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)
        pmd.addTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))

        // corrupt the employees data file
        File("$DEFAULT_DB_DIRECTORY/employees.json").writeText("BAD DATA HERE")
        
        val ex = assertThrows(DatabaseCorruptedException::class.java) {PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)}
        assertEquals("Could not read employees file. Unexpected JSON token at offset 0: Expected '[, kind: LIST' - JSON input: BAD DATA HERE", ex.message)
    }

    /**
     * What if we see corruption in our database by dint of missing files
     * that definitely should not be missing?  I think the most appropriate
     * response is to halt and yell for help, because at that point all
     * bets are off, , we won't have enough information to properly recover.
     */
    @Test
    fun testPersistence_Read_CorruptedData_Employees_MissingFile() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)
        pmd.addTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))

        // delete a necessary file
        File("$DEFAULT_DB_DIRECTORY/employees.json").delete()

        val ex = assertThrows(DatabaseCorruptedException::class.java) {PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)}
        assertEquals("Unable to find an employee with the id of 2 based on entry in timeentries/", ex.message)
    }

    /**
     * In the time entries directory we store files by employee id.  If we look inside
     * a directory and find no files, that indicates a data corruption.
     */
    @Test
    fun testPersistence_Read_CorruptedData_TimeEntries_MissingFile() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)
        pmd.addTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))

        // delete a necessary time entry file inside this employees' directory
        File("$DEFAULT_DB_DIRECTORY/timeentries/${newEmployee.id.value}/").listFiles()?.forEach { it.delete() }

        assertThrows(DatabaseCorruptedException::class.java) {PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)}
    }
    
    /**
     * See [testPersistence_Read_CorruptedData_TimeEntries_BadData]
     */
    @Test
    fun testPersistence_Read_CorruptedData_Users_BadData() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)
        pmd.addTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))

        // corrupt the users data file
        File("$DEFAULT_DB_DIRECTORY/users.json").writeText("BAD DATA HERE")
        
        assertThrows(DatabaseCorruptedException::class.java) {PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)}
    }
    
    /**
     * See [testPersistence_Read_CorruptedData_Employees_MissingFile]
     */
    @Test
    fun testPersistence_Read_CorruptedData_Users_MissingFile() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)
        pmd.addTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))

        // delete a necessary file
        File("$DEFAULT_DB_DIRECTORY/users.json").delete()

        val ex = assertThrows(DatabaseCorruptedException::class.java) {PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)}
        assertEquals("Unable to find a user with the id of 1.  User set size: 0", ex.message)
    }
    
    /**
     * See [testPersistence_Read_CorruptedData_TimeEntries_BadData]
     */
    @Test
    fun testPersistence_Read_CorruptedData_Projects_BadData() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)
        pmd.addTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))

        // corrupt the projects data file
        File("$DEFAULT_DB_DIRECTORY/projects.json").writeText("BAD DATA HERE")
        
        val ex = assertThrows(DatabaseCorruptedException::class.java) {PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)}
        assertEquals("Could not read projects file. Unexpected JSON token at offset 0: Expected '[, kind: LIST' - JSON input: BAD DATA HERE", ex.message)
    }
    
    /**
     * See [testPersistence_Read_CorruptedData_Employees_MissingFile]
     */
    @Test
    fun testPersistence_Read_CorruptedData_Projects_MissingFile() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)
        pmd.addTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))

        // delete a necessary file
        File("$DEFAULT_DB_DIRECTORY/projects.json").delete()

        val ex = assertThrows(DatabaseCorruptedException::class.java) {PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)}
        assertEquals("Unable to find a project with the id of 1.  Project set size: 0", ex.message)
    }
    
    /**
     * See [testPersistence_Read_CorruptedData_TimeEntries_BadData]
     */
    @Test
    fun testPersistence_Read_CorruptedData_Sessions_BadData() {
        File(DEFAULT_DB_DIRECTORY).deleteRecursively()
        val pmd = PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)
        val newEmployee = pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, newEmployee.id)
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, newUser, DEFAULT_DATETIME)
        pmd.addTimeEntry(createTimeEntryPreDatabase(employee = newEmployee, project = newProject))

        // corrupt the time-entries data file
        File("$DEFAULT_DB_DIRECTORY/sessions.json").writeText("BAD DATA HERE")
        
        assertThrows(DatabaseCorruptedException::class.java) {PureMemoryDatabase.start(DEFAULT_DB_DIRECTORY)}
    }


    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    private fun prepareSomeRandomTimeEntries(numTimeEntries: Int, projects: MutableSet<Project>, employees: MutableSet<Employee>): MutableSet<TimeEntry> {
        val timeEntries: MutableSet<TimeEntry> = mutableSetOf()
        for (i in 1..numTimeEntries) {
            timeEntries.add(
                TimeEntry(
                    i,
                    employees.first(),
                    projects.first(),
                    Time(100),
                    A_RANDOM_DAY_IN_JUNE_2020,
                    Details("I was lazing on a sunday afternoon")
                )
            )
        }
        return timeEntries
    }

    private fun recordManyTimeEntries(numberOfEmployees: Int, numberOfProjects: Int, numberOfDays: Int) : List<Employee> {
        val lotsOfEmployees: List<String> = generateEmployeeNames()
        persistEmployeesToDatabase(numberOfEmployees, lotsOfEmployees)
        val allEmployees: List<Employee> = readEmployeesFromDatabase()
        persistProjectsToDatabase(numberOfProjects)
        val allProjects: List<Project> = readProjectsFromDatabase()
        enterTimeEntries(numberOfDays, allEmployees, allProjects, numberOfEmployees)
        return allEmployees
    }

    private fun accumulateMinutesPerEachEmployee(allEmployees: List<Employee>) {
        val (timeToAccumulate) = getTime {
            val minutesPerEmployeeTotal =
                    allEmployees.map { e -> pmd.getAllTimeEntriesForEmployee(e).sumBy { te -> te.time.numberOfMinutes } }
                            .toList()
            logAudit("the time ${allEmployees[0].name.value} spent was ${minutesPerEmployeeTotal[0]}")
            logAudit("the time ${allEmployees[1].name.value} spent was ${minutesPerEmployeeTotal[1]}")
        }

        logAudit("It took $timeToAccumulate milliseconds to accumulate the minutes per employee")
    }

    private fun readTimeEntriesForOneEmployee(allEmployees: List<Employee>) {
        val (timeToGetAllTimeEntries) = getTime { pmd.getAllTimeEntriesForEmployee(allEmployees[0]) }
        logAudit("It took $timeToGetAllTimeEntries milliseconds to get all the time entries for a employee")
    }

    private fun enterTimeEntries(numberOfDays: Int, allEmployees: List<Employee>, allProjects: List<Project>, numberOfEmployees: Int) {
        val (timeToEnterAllTimeEntries) = getTime {
            for (day in 1..numberOfDays) {
                for (employee in allEmployees) {
                    pmd.addTimeEntry(TimeEntryPreDatabase(employee, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                    pmd.addTimeEntry(TimeEntryPreDatabase(employee, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                    pmd.addTimeEntry(TimeEntryPreDatabase(employee, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                    pmd.addTimeEntry(TimeEntryPreDatabase(employee, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                }
            }
        }
        logAudit("It took $timeToEnterAllTimeEntries milliseconds total to enter ${numberOfDays * 4} time entries for each of $numberOfEmployees employees")
        logAudit("(That's a total of ${("%,d".format(numberOfDays * 4 * numberOfEmployees))} time entries)")
    }

    private fun readProjectsFromDatabase(): List<Project> {
        val (timeToReadAllProjects, allProjects) = getTime { pmd.getAllProjects()}
        logAudit("It took $timeToReadAllProjects milliseconds to read all the projects")
        return allProjects
    }

    private fun persistProjectsToDatabase(numberOfProjects: Int) {
        val (timeToCreateProjects) =
                getTime { (1..numberOfProjects).forEach { i -> pmd.addNewProject(ProjectName("project$i")) } }
        logAudit("It took $timeToCreateProjects milliseconds to create $numberOfProjects projects")
    }

    private fun readEmployeesFromDatabase(): List<Employee> {
        val (timeToReadAllEmployees, allEmployees) = getTime {
            pmd.getAllEmployees()
        }
        logAudit("It took $timeToReadAllEmployees milliseconds to read all the employees")
        return allEmployees
    }

    private fun persistEmployeesToDatabase(numberOfEmployees: Int, lotsOfEmployees: List<String>) {
        val (timeToEnterEmployees) = getTime {
            for (i in 1..numberOfEmployees) {
                pmd.addNewEmployee(EmployeeName(lotsOfEmployees[i]))
            }
        }
        logAudit("It took $timeToEnterEmployees milliseconds to enter $numberOfEmployees employees")
    }

    private fun generateEmployeeNames(): List<String> {
        val (timeToMakeEmployeenames, lotsOfEmployees) = getTime {
            listOf(
                    "Arlen", "Hedwig", "Allix", "Tandi", "Silvia", "Catherine", "Mavis", "Hally", "Renate", "Anastasia", "Christy", "Nora", "Molly", "Nelli", "Daphna", "Chloette", "TEirtza", "Nannie", "Melinda", "Tyne", "Belva", "Pam", "Rebekkah", "Elayne", "Dianne", "Christina", "Jeanne", "Norry", "Reina", "Erminia", "Eadie", "Valina", "Gayle", "Wylma", "Annette", "Annmaria", "Fayina", "Dita", "Sibella", "Alis", "Georgena", "Luciana", "Sidonnie", "Dina", "Ferdinande", "Coletta", "Esma", "Saidee", "Hannah", "Colette", "Anitra", "Grissel", "Caritta", "Ann", "Rhodia", "Meta", "Bride", "Dalenna", "Rozina", "Ottilie", "Eliza", "Gerda", "Anthia", "Kathryn", "Lilian", "Jeannie", "Nichole", "Marylinda", "Angelica", "Margie", "Ruthie", "Augustina", "Netta", "Fleur", "Mahala", "Cosette", "Zsa Zsa", "Karry", "Tabitha", "Andriana", "Fey", "Hedy", "Saudra", "Geneva", "Lacey", "Fawnia", "Ertha", "Bernie", "Natty", "Joyan", "Teddie", "Hephzibah", "Vonni", "Ambur", "Lyndsie", "Anna", "Minnaminnie", "Andy", "Brina", "Pamella", "Trista", "Antonetta", "Kerrin", "Crysta", "Kira", "Gypsy", "Candy", "Ree", "Sharai", "Mariana", "Eleni", "Yetty", "Maisie", "Deborah", "Doretta", "Juliette", "Esta", "Amandi", "Anallise", "Indira", "Aura", "Melodee", "Desiri", "Jacquenetta", "Joell", "Delcine", "Justine", "Theresita", "Belia", "Mallory", "Antonie", "Jobi", "Katalin", "Kelli", "Ester", "Katey", "Gianna", "Berry", "Sidonia", "Roseanne", "Cherida", "Beatriz", "Eartha", "Robina", "Florri", "Vitoria", "Debera", "Jeanette", "Almire", "Saree", "Liana", "Ruth", "Renell", "Katinka", "Anya", "Gwyn", "Kaycee", "Rori", "Rianon", "Joann", "Zorana", "Hermia", "Gwenni", "Poppy", "Dedie", "Cloe", "Kirsti", "Krysta", "Clarinda", "Enid", "Katina", "Ralina", "Meryl", "Andie", "Orella", "Alexia", "Clarey", "Iris", "Chris", "Devin", "Kally", "Vernice", "Noelyn", "Stephana", "Catina", "Faydra", "Fionna", "Nola", "Courtnay", "Vera", "Meriel", "Eleonora", "Clare", "Marsha", "Marita", "Concettina", "Kristien", "Celina", "Maryl", "Codee", "Lorraine", "Lauraine", "Sephira", "Kym", "Odette", "Ranee", "Margaux", "Debra", "Corenda", "Mariejeanne", "Georgeanne", "Laural", "Fredelia", "Dulcine", "Tess", "Tina", "Adaline", "Melisandra", "Lita", "Nettie", "Lesley", "Clea", "Marysa", "Arleyne", "Meade", "Ella", "Theodora", "Morgan", "Carena", "Camille", "Janene", "Anett", "Camellia", "Guglielma", "Evvy", "Shayna", "Karilynn", "Ingeberg", "Maudie", "Colene", "Kelcy", "Blythe", "Lacy", "Cesya", "Bobbe", "Maggi", "Darline", "Almira", "Constantia", "Helaina", "Merrili", "Maxine", "Linea", "Marley", "Timmie", "Devon", "Mair", "Thomasine", "Sherry", "Gilli", "Ursa", "Marlena", "Cloris", "Vale", "Alexandra", "Angel", "Alice", "Ulrica", "Britteny", "Annie", "Juliane", "Candida", "Jennie", "Susanne", "Robenia", "Benny", "Cassy", "Denyse", "Jackquelin", "Lorelle", "Lenore", "Sheryl", "Marice", "Clarissa", "Kippy", "Cristen", "Hanni", "Marne", "Melody", "Shane", "Kalli", "Deane", "Kaila", "Faye", "Noella", "Penni", "Sophia", "Marilin", "Cori", "Clair", "Morna", "Lynn", "Rozelle", "Berta", "Bamby", "Janifer", "Doro", "Beryle", "Pammy", "Paige", "Juanita", "Ellene", "Kora", "Kerrie", "Perrine", "Dorena", "Mady", "Dorian", "Lucine", "Jill", "Octavia", "Sande", "Talyah", "Rafaelia", "Doris", "Patti", "Mora", "Marja", "Rivi", "Drucill", "Marina", "Rennie", "Annabell", "Xylia", "Zorina", "Ashil", "Becka", "Blithe", "Lenora", "Kattie", "Analise", "Jasmin", "Minetta", "Deeanne", "Sharity", "Merci", "Julissa", "Nicoli", "Nevsa", "Friederike", "Caroljean", "Catlee", "Charil", "Dara", "Kristy", "Ag", "Andriette", "Kati", "Jackqueline", "Letti", "Allys", "Carlee", "Frannie", "Philis", "Aili", "Else", "Diane", "Tobey", "Tildie", "Merrilee", "Pearle", "Christan", "Dominique", "Rosemaria", "Bunnie", "Tedi", "Elinor", "Aeriell", "Karissa", "Darya", "Tonye", "Alina", "Nalani", "Marcela", "Anabelle", "Layne", "Dorice", "Aleda", "Anette", "Arliene", "Rosemarie", "Pru", "Tiffani", "Addi", "Roda", "Shandra", "Wendeline", "Karoline", "Ciel", "Ania"
            )
            // if you want to make a lot more names, uncomment below
            // lotsOfEmployees = (1..10).flatMap { n -> employeenames.map { u -> "$u$n" } }.toList()
        }
        logAudit("It took $timeToMakeEmployeenames milliseconds to create ${lotsOfEmployees.size} employeenames")
        return lotsOfEmployees
    }

}