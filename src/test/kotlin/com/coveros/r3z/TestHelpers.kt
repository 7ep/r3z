package com.coveros.r3z
import com.coveros.r3z.domainobjects.*


/**
 * a test helper method to create a [TimeEntry]
 */
fun createTimeEntry(
        user : User = User(1, "I"),
        time : Time = Time(60),
        project : Project = Project(1, "A"),
        details : Details = Details()
): TimeEntry {
    return TimeEntry(user, project, time, details)
}