package coverosR3z
import coverosR3z.authentication.AuthenticationPersistence
import coverosR3z.authentication.AuthenticationUtilities
import coverosR3z.authentication.CurrentUser
import coverosR3z.domainobjects.*
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.timerecording.ITimeEntryPersistence
import coverosR3z.timerecording.TimeEntryPersistence
import coverosR3z.timerecording.TimeRecordingUtilities
import org.junit.Assert

/**
 * a test helper method to create a [TimeEntry]
 */

const val DEFAULT_DB_DIRECTORY = "build/db/"
val A_RANDOM_DAY_IN_JUNE_2020 = Date(2020, Month.JUN, 25)
val A_RANDOM_DAY_IN_JUNE_2020_PLUS_ONE = Date(2020, Month.JUN, 26)
val DEFAULT_DATETIME = DateTime(2020, Month.JAN, 1, 0, 0, 0)
val THREE_HOURS_FIFTEEN = Time((3 * 60) + 15)
val DEFAULT_SALT = Salt("12345")
val DEFAULT_PASSWORD = Password("password1234")
val DEFAULT_HASH = Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT)
const val DEFAULT_HASH_STRING = "4dc91e9a80320c901f51ccf7166d646c"
val DEFAULT_USER = User(UserId(1), UserName("DefaultUser"), DEFAULT_HASH, DEFAULT_SALT, EmployeeId(1))
val DEFAULT_EMPLOYEE_NAME = EmployeeName("DefaultEmployee")
val DEFAULT_EMPLOYEE = Employee(EmployeeId(1), DEFAULT_EMPLOYEE_NAME)
val DEFAULT_TIME = Time(60)
val DEFAULT_PROJECT_NAME = ProjectName("Default_Project")
val DEFAULT_PROJECT = Project(ProjectId(1), DEFAULT_PROJECT_NAME)
const val DEFAULT_SESSION_TOKEN = "abc123"

fun createTimeEntryPreDatabase(
        employee: Employee = DEFAULT_EMPLOYEE,
        time: Time = DEFAULT_TIME,
        project: Project = DEFAULT_PROJECT,
        details: Details = Details(),
        date: Date = A_RANDOM_DAY_IN_JUNE_2020
) = TimeEntryPreDatabase ( employee, project, time, date, details)

/**
 * A test helper method to generate a [TimeRecordingUtilities]
 * with a real database connected
 */
fun createTimeRecordingUtility(user : User = SYSTEM_USER): TimeRecordingUtilities {
        val timeEntryPersistence : ITimeEntryPersistence = TimeEntryPersistence(PureMemoryDatabase())
        return TimeRecordingUtilities(timeEntryPersistence, CurrentUser(user))
}

/**
 * Create an employee, "Alice", register a user for her, create a project
 */
fun initializeAUserAndLogin() : Pair<TimeRecordingUtilities, Employee>{
        val pmd = PureMemoryDatabase()
        val authPersistence = AuthenticationPersistence(pmd)
        val au = AuthenticationUtilities(authPersistence)

        val systemTru = TimeRecordingUtilities(TimeEntryPersistence(pmd), CurrentUser(SYSTEM_USER))
        val aliceEmployee = systemTru.createEmployee(EmployeeName("Alice"))

        au.register(UserName("alice"), DEFAULT_PASSWORD, aliceEmployee.id)
        val (_, aliceUser) = au.login(UserName("alice"), DEFAULT_PASSWORD)

        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd), CurrentUser(aliceUser))

        // Perform some quick checks
        Assert.assertTrue("Registration must have succeeded", au.isUserRegistered(UserName("alice")))

        tru.createProject(DEFAULT_PROJECT_NAME)

        return Pair(tru, aliceEmployee)
}

