package com.coveros.r3z
import com.coveros.r3z.domainobjects.*

/**
 * a test helper method to create a [TimeEntry]
 */

val A_RANDOM_DAY_IN_JUNE_2020 = Date(2020, Month.JUN, 25)
val THREE_HOURS_FIFTEEN = Time((3 * 60) + 15)

fun createTimeEntry(
    user: User = User(1, "I"),
    time: Time = Time(60),
    project: Project = Project(1, "A"),
    details: Details = Details(),
    date: Date = A_RANDOM_DAY_IN_JUNE_2020
): TimeEntry {
    return TimeEntry(user, project, time, date, details)
}