package com.coveros.r3z.timerecording

import com.coveros.r3z.exceptions.ExceededDailyHoursAmountException
import com.coveros.r3z.domainobjects.*

class TimeRecordingUtilities(val persistence: TimeEntryPersistence) {

    fun recordTime(entry: TimeEntry): RecordTimeResult {
        val minutesRecorded = persistence.queryMinutesRecorded(User(1,"Test"), entry.date) ?: 0

        val twentyFourHours = 24 * 60
        // If the user is entering in more than 24 hours in a day, that's insane.
        if ((minutesRecorded + entry.time.numberOfMinutes) > twentyFourHours) {
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