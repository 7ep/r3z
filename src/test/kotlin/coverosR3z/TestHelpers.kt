package coverosR3z
import coverosR3z.domainobjects.*

/**
 * a test helper method to create a [TimeEntry]
 */

val A_RANDOM_DAY_IN_JUNE_2020 = Date(2020, Month.JUN, 25)
val A_RANDOM_DAY_IN_JUNE_2020_PLUS_ONE = Date(2020, Month.JUN, 26)
val THREE_HOURS_FIFTEEN = Time((3 * 60) + 15)
val DEFAULT_USER = User(1, "I")
val DEFAULT_USERNAME = UserName("I")
val DEFAULT_TIME = Time(60)
val DEFAULT_PROJECT = Project(1, "A")
val DEFAULT_PROJECT_NAME = ProjectName("A")

fun createTimeEntryPreDatabase(
        user: User = DEFAULT_USER,
        time: Time = DEFAULT_TIME,
        project: Project = DEFAULT_PROJECT,
        details: Details = Details(),
        date: Date = A_RANDOM_DAY_IN_JUNE_2020
) = TimeEntryPreDatabase ( user, project, time, date, details)

/**
 * returns the time spent on the items inside.
 * To use: simply wrap the code with getTime, like this:
 *
 *      val timeTaken = getTime {
 *           foo()
 *           bar()
 *      }
 */
fun getTime(function: () -> Unit): Long {
        val start = System.currentTimeMillis()
        function()
        val finish = System.currentTimeMillis()
        return finish - start
}

/**
 * Makes it easy to access data in the resource directory
 */
fun getResourceAsText(path: String): String {
        return object {}.javaClass.getResource(path).readText()
}