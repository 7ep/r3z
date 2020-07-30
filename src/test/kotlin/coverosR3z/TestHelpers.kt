package coverosR3z
import coverosR3z.domainobjects.*
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.timerecording.ITimeEntryPersistence
import coverosR3z.timerecording.TimeEntryPersistence
import coverosR3z.timerecording.TimeRecordingUtilities
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

/**
 * a test helper method to create a [TimeEntry]
 */

val A_RANDOM_DAY_IN_JUNE_2020 = Date(2020, Month.JUN, 25)
val A_RANDOM_DAY_IN_JUNE_2020_PLUS_ONE = Date(2020, Month.JUN, 26)
val THREE_HOURS_FIFTEEN = Time((3 * 60) + 15)
val DEFAULT_EMPLOYEE = Employee(1, "I")
val DEFAULT_EMPLOYEENAME = EmployeeName("I")
val DEFAULT_TIME = Time(60)
val DEFAULT_PROJECT = Project(1, "A")
val DEFAULT_PROJECT_NAME = ProjectName("A")

fun createTimeEntryPreDatabase(
        employee: Employee = DEFAULT_EMPLOYEE,
        time: Time = DEFAULT_TIME,
        project: Project = DEFAULT_PROJECT,
        details: Details = Details(),
        date: Date = A_RANDOM_DAY_IN_JUNE_2020
) = TimeEntryPreDatabase ( employee, project, time, date, details)

/**
 * returns the time spent on the items inside.
 * To use: simply wrap the code with getTime, like this:
 *
 *      val timeTaken = getTime {
 *           foo()
 *           bar()
 *      }
 */
fun <T>getTime(function: () -> T): Pair<Long, T> {
        val start = System.currentTimeMillis()
        val result : T = function()
        val finish = System.currentTimeMillis()
        return Pair(finish - start, result)
}

/**
 * Makes it easy to access data in the resource directory
 * This is to read text from a file in the resource directory
 */
fun getResourceAsText(path: String): String {
        return object {}.javaClass.getResource(path).readText()
}

/**
 * This is useful for tests that require serialization
 * using Kotlin's own serialization framework.
 *
 * See https://github.com/Kotlin/kotlinx.serialization
 */
val jsonSerialzation : Json = Json(JsonConfiguration.Stable)
val jsonSerialzationWithPrettyPrint : Json = Json(JsonConfiguration.Stable.copy(prettyPrint = true))

/**
 * A test helper method to generate a [TimeRecordingUtilities]
 * with a real database connected
 */
fun createTimeRecordingUtility(): TimeRecordingUtilities {
        val timeEntryPersistence : ITimeEntryPersistence = TimeEntryPersistence(PureMemoryDatabase())
        return TimeRecordingUtilities(timeEntryPersistence)
}