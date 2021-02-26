package coverosR3z.misc
import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.authentication.utility.FakeRolesChecker
import coverosR3z.logging.TestLogger
import coverosR3z.misc.types.Date
import coverosR3z.misc.types.DateTime
import coverosR3z.misc.types.Month
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.timerecording.FakeTimeEntryPersistence
import coverosR3z.timerecording.persistence.ITimeEntryPersistence
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert

/**
 * a test helper method to create a [TimeEntry]
 */

const val DEFAULT_DB_DIRECTORY = "build/db/"
val A_RANDOM_DAY_IN_JUNE_2020 = Date(2020, Month.JUN, 25)
val A_RANDOM_DAY_IN_JUNE_2020_PLUS_ONE = Date(2020, Month.JUN, 26)

/**
 * HTML5 sends dates in this format
 */
const val DEFAULT_DATE_STRING = "2020-06-12"
val DEFAULT_DATE = Date.make(DEFAULT_DATE_STRING)
val DEFAULT_DATETIME = DateTime(2020, Month.JAN, 1, 0, 0, 0)
val THREE_HOURS_FIFTEEN = Time((3 * 60) + 15)
val DEFAULT_SALT = Salt("12345")
val DEFAULT_PASSWORD = Password("password1234")
val DEFAULT_HASH = Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT)
const val DEFAULT_HASH_STRING = "4dc91e9a80320c901f51ccf7166d646c"
val DEFAULT_EMPLOYEE_NAME = EmployeeName("DefaultEmployee")
val DEFAULT_EMPLOYEE = Employee(EmployeeId(1), DEFAULT_EMPLOYEE_NAME)
val DEFAULT_USER = User(UserId(1), UserName("DefaultUser"), DEFAULT_HASH, DEFAULT_SALT, DEFAULT_EMPLOYEE.id)
val DEFAULT_REGULAR_USER = User(UserId(1), UserName("DefaultUser"), DEFAULT_HASH, DEFAULT_SALT, EmployeeId(1), role=Roles.REGULAR)
val DEFAULT_ADMIN_USER = User(UserId(1), UserName("DefaultAdminUser"), DEFAULT_HASH, DEFAULT_SALT, EmployeeId(1), role=Roles.ADMIN)
val DEFAULT_USER_2 = User(UserId(2), UserName("DefaultUser2"), DEFAULT_HASH, DEFAULT_SALT, EmployeeId(2))
val DEFAULT_USER_SYSTEM_EMPLOYEE = User(UserId(2), UserName("DefaultUser2"), DEFAULT_HASH, DEFAULT_SALT, EmployeeId(0))
val DEFAULT_TIME = Time(60)
val DEFAULT_PROJECT_NAME = ProjectName("Default_Project")
val DEFAULT_PROJECT = Project(ProjectId(1), DEFAULT_PROJECT_NAME)
val DEFAULT_TIME_ENTRY = TimeEntry(TimeEntryId(1), DEFAULT_EMPLOYEE, DEFAULT_PROJECT, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020)
val DEFAULT_PERIOD_START_DATE = Date.make( "2021-02-01")
val DEFAULT_PERIOD_END_DATE = Date.make( "2021-02-15")
val DEFAULT_TIME_PERIOD = TimePeriod(DEFAULT_PERIOD_START_DATE, DEFAULT_PERIOD_END_DATE)
val DEFAULT_SUBMITTED_PERIOD = SubmittedPeriod(SubmissionId(1), DEFAULT_EMPLOYEE.id, DEFAULT_TIME_PERIOD)
const val DEFAULT_SESSION_TOKEN = "abc123"
const val granularPerfArchiveDirectory = "docs/performance_archive/granular_tests/"
val testLogger = TestLogger()

/**
 * Helper to easily put together a time entry
 */
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
fun createTimeRecordingUtility(user : User = DEFAULT_ADMIN_USER): TimeRecordingUtilities {
        val timeEntryPersistence : ITimeEntryPersistence = TimeEntryPersistence(PureMemoryDatabase(), logger = testLogger)
        return TimeRecordingUtilities(timeEntryPersistence, CurrentUser(user), testLogger)
}

/**
 * Create an employee, "Alice", register a user for her, create a project
 */
fun initializeAUserAndLogin() : Triple<TimeRecordingUtilities, Employee, Employee>{
        val pmd = PureMemoryDatabase()
        val authPersistence = AuthenticationPersistence(pmd, testLogger)
        val au = AuthenticationUtilities(authPersistence, testLogger)

        val adminTru = TimeRecordingUtilities(TimeEntryPersistence(pmd, logger = testLogger), CurrentUser(DEFAULT_ADMIN_USER), testLogger)
        val aliceEmployee = adminTru.createEmployee(EmployeeName("Alice"))
        val sarahEmployee = adminTru.createEmployee(EmployeeName("Sarah"))
        adminTru.createProject(DEFAULT_PROJECT_NAME)

        au.register(UserName("alice"), DEFAULT_PASSWORD, aliceEmployee.id)
        val (_, aliceUser) = au.login(UserName("alice"), DEFAULT_PASSWORD)

        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd, logger = testLogger), CurrentUser(aliceUser), testLogger)
        // Perform some quick checks
        Assert.assertTrue("Registration must have succeeded", au.isUserRegistered(UserName("alice")))

        return Triple(tru, aliceEmployee, sarahEmployee)
}
