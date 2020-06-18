package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.TimeEntry
import com.coveros.r3z.persistence.microorm.DbAccessHelper
import com.coveros.r3z.persistence.microorm.IDbAccessHelper
import javax.sql.DataSource

class TimeEntryPersistence(ds: DataSource) {

    private val db: IDbAccessHelper = DbAccessHelper(ds)

    fun persistNewTimeEntry(entry: TimeEntry): Long {
        return db.executeInsert(
                "Creates a new user in the database",
                "INSERT INTO TIME.TIMEENTRY (user, project, time_in_minutes, details) VALUES (?, ?, ?, ?);",
                entry.user.id, entry.project.id, entry.time, entry.details)
    }
}