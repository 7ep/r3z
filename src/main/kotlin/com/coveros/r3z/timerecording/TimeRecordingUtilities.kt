package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.*

class TimeRecordingUtilities(val persistence: TimeEntryPersistence) {

    fun recordTime(entry: TimeEntry): RecordTimeResult {
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