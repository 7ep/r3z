package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.TimeEntry
import com.coveros.r3z.persistence.microorm.IDbAccessHelper

class TimeEntryPersistence(private val dbHelper: IDbAccessHelper) {

    fun persistNewTimeEntry(entry: TimeEntry): Long {
        return dbHelper.executeInsert(
                "Creates a new user in the database",
                "INSERT INTO TIME.TIMEENTRY (user, project, time_in_minutes, details) VALUES (?, ?, ?, ?);",
                entry.user.id, entry.project.id, entry.time, entry.details)
    }

}