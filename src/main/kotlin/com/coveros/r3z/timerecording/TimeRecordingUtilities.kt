package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.Project
import com.coveros.r3z.domainobjects.RecordTimeResult
import com.coveros.r3z.domainobjects.StatusEnum
import com.coveros.r3z.domainobjects.TimeEntry

fun recordTime(entry : TimeEntry) : RecordTimeResult {
    val stringRepresentation = "ID: " + entry.user.id + "Time: " + entry.time.numberOfMinutes + "\nDetails: " + entry.details.value

    // put in database
    // store success or failure of action
    // spit back out database id of the time entry's row

    val dbId = 0

    if (isValidProject(entry.project)) {
        return RecordTimeResult(id = dbId, status = StatusEnum.SUCCESS)
    }
    return RecordTimeResult(id = null, status = StatusEnum.INVALID_PROJECT)
}

fun isValidProject(project : Project) : Boolean {
    if (project.name == "an invalid project") {
        return false
    }
    return true
}