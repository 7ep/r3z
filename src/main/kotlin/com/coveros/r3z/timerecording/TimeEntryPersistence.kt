package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.persistence.microorm.IDbAccessHelper
import java.sql.ResultSet

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

    /**
     * Provided a user and date, give the number of minutes they worked on that date
     */
    fun queryMinutesRecorded(user: User, date: Date): Long? {
        return dbHelper.runQuery(
                "To restrict impossible states for users, it's necessary to check total hours recorded w/in criteria",
                "SELECT SUM (TIME_IN_MINUTES) AS total FROM TIMEANDEXPENSES.TIMEENTRY WHERE user=(?);",
                { r -> r.getLong("total")},
                user.id)
    }

    fun readTimeEntries(user: User): List<TimeEntry>? {


        val extractor: (ResultSet) -> List<TimeEntry> = { r ->
            val myList: MutableList<TimeEntry> = mutableListOf()
            while (r.next()) {
                myList.add(
                    TimeEntry(
                        User(r.getLong("user"), ""),
                        Project(r.getLong("project"), ""),
                        Time(r.getInt("time")),
                        Date(r.getDate("date")),
                        Details(r.getString("details"))
                    )
                )
            }
            myList.toList()
        }
        return dbHelper.runQuery("For validation, it is sometimes necessary to look up existing entries",
            "SELECT * FROM TIMEANDEXPENSES.TIMEENETRY WHERE user=(?);",
            extractor,
            user.id)
    }
}