package coverosR3z
import coverosR3z.domainobjects.*

/**
 * a test helper method to create a [TimeEntry]
 */

val A_RANDOM_DAY_IN_JUNE_2020 = Date(2020, Month.JUN, 25)
val A_RANDOM_DAY_IN_JUNE_2020_PLUS_ONE = Date(2020, Month.JUN, 26)
val THREE_HOURS_FIFTEEN = Time((3 * 60) + 15)
val DEFAULT_USER = User(1, "I")
val DEFAULT_TIME = Time(60)
val DEFAULT_PROJECT = Project(1, "A")

fun createTimeEntry(
        id : Int = 1,
        user: User = DEFAULT_USER,
        time: Time = DEFAULT_TIME,
        project: Project = DEFAULT_PROJECT,
        details: Details = Details(),
        date: Date = A_RANDOM_DAY_IN_JUNE_2020
): TimeEntry {
    return TimeEntry(id, user, project, time, date, details)
}