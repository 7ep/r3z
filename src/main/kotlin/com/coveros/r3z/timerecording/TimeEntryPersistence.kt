package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.Project
import com.coveros.r3z.domainobjects.ProjectName
import com.coveros.r3z.domainobjects.TimeEntry
import com.coveros.r3z.persistence.microorm.IDbAccessHelper

class TimeEntryPersistence(private val dbHelper: IDbAccessHelper) {

    fun persistNewTimeEntry(entry: TimeEntry): Long {
        return dbHelper.executeInsert(
                "Creates a new time entry in the database - a record of a particular users's time on a project",
                "INSERT INTO TIMEANDEXPENSES.TIMEENTRY (user, project, time_in_minutes, details) VALUES (?, ?, ?, ?);",
                entry.user.id, entry.project.id, entry.time.numberOfMinutes, entry.details.value)
    }

    fun persistNewProject(projectName: ProjectName) : Project {
        val id = dbHelper.executeInsert(
                "record a new project, the database will give us its id",
                "INSERT INTO TIMEANDEXPENSES.PROJECT (name) VALUES (?);",
                projectName.value)
        return Project(id, projectName.value)
    }
}