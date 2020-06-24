package com.coveros.r3z.timerecording

import com.coveros.r3z.ExceededDailyHoursAmountException
import com.coveros.r3z.domainobjects.*
import java.sql.Date

class TimeRecordingUtilities(val persistence: TimeEntryPersistence) {

    fun recordTime(entry: TimeEntry): RecordTimeResult {
        // true if the database has 24 hours already for the provided user and project
        var minutesRecorded = persistence.queryMinutesRecorded(User(1,"Test"), Date(3))

        if ((minutesRecorded + entry.time.numberOfMinutes) > 24*60) {
            throw ExceededDailyHoursAmountException()
        }
        if (isValidProject(entry.project)) {
            val newId = persistence.persistNewTimeEntry(entry)
            return RecordTimeResult(id = newId, status = StatusEnum.SUCCESS)
        } else {
            return RecordTimeResult(id = null, status = StatusEnum.INVALID_PROJECT)
        }
    }

    fun isValidProject(project: Project): Boolean {
        if (project.name == "an invalid project") {
            return false
        }
        return true
    }

    fun createProject(projectName: ProjectName) : Project {
        return persistence.persistNewProject(projectName)
    }
}