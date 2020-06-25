package com.coveros.r3z
import com.coveros.r3z.domainobjects.*
import java.time.LocalDate

/**
 * a test helper method to create a [TimeEntry]
 */

var date = LocalDate.parse("2018-12-12")

fun createTimeEntry(
    user: User = User(1, "I"),
    time: Time = Time(60),
    project: Project = Project(1, "A"),
    details: Details = Details(),
    date: Date = Date("2020-6-25")
): TimeEntry {
    return TimeEntry(user, project, time, details)
}