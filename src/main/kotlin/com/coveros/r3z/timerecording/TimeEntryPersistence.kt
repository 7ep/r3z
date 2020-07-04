package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.persistence.microorm.IDbAccessHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.ResultSet

class TimeEntryPersistence(private val dbHelper: IDbAccessHelper) {

    companion object {
        val log : Logger = LoggerFactory.getLogger(TimeEntryPersistence::class.java)
    }

    fun persistNewTimeEntry(entry: TimeEntry): Long {
        return dbHelper.executeUpdate(
                "Creates a new time entry in the database - a record of a particular users's time on a project",
                "INSERT INTO TIMEANDEXPENSES.TIMEENTRY (user, project, time_in_minutes, date, details) VALUES (?, ?,?, ?, ?);",
                entry.user.id, entry.project.id, entry.time.numberOfMinutes, entry.date.sqlDate, entry.details.value)
    }

    fun persistNewProject(projectName: ProjectName) : Project {
        assert(projectName.value.isNotEmpty()) {"Project name cannot be empty"}
        log.info("Recording a new project, ${projectName.value}, to the database")

        val id = dbHelper.executeUpdate(
                "record a new project, the database will give us its id",
                "INSERT INTO TIMEANDEXPENSES.PROJECT (name) VALUES (?);",
                projectName.value)

        assert(id > 0) {"A valid project will receive a positive id"}
        return Project(id, projectName.value)
    }

    fun persistNewUser(username: String): User {
        assert(username.isNotEmpty())
        log.info("Recording a new user, $username, to the database")

        val newId = dbHelper.executeUpdate(
            "Creates a new user in the database",
            "INSERT INTO TIMEANDEXPENSES.PERSON (name) VALUES (?);", username)

        assert(newId > 0) {"A valid user will receive a positive id"}
        return User(newId, username)
    }

    /**
     * Provided a user and date, give the number of minutes they worked on that date
     */
    fun queryMinutesRecorded(user: User, date: Date): Long? {
        return dbHelper.runQuery(
                "To restrict impossible states for users, it's necessary to check total hours recorded w/in criteria",
                "SELECT SUM (TIME_IN_MINUTES) AS total FROM TIMEANDEXPENSES.TIMEENTRY WHERE user=(?) AND date=(?);",
                { r -> r.getLong("total")},
                user.id, date.sqlDate)
    }

    fun readTimeEntries(user: User): List<TimeEntry>? {


        val extractor: (ResultSet) -> List<TimeEntry> = { r ->
            val myList: MutableList<TimeEntry> = mutableListOf()
            do {
                myList.add(
                    TimeEntry(
                        User(r.getLong("userid"), r.getString("username")),
                        Project(r.getLong("projectid"), r.getString("projectname")),
                        Time(r.getInt("time")),
                        Date.convertSqlDateToOurDate(r.getDate("date")),
                        Details(r.getString("details"))
                    )
                )
            } while (r.next())
            myList.toList()
        }
        return dbHelper.runQuery("For validation, it is sometimes necessary to look up existing entries",
            """
        SELECT te.user as userid, p.name as username, te.project as projectid, pj.name as projectname, 
            te.time_in_minutes as time, te.date, te.details 
                FROM TIMEANDEXPENSES.TIMEENTRY as te 
                JOIN timeandexpenses.person as p on te.user = p.id
                JOIN timeandexpenses.project as pj on te.project = pj.id
                WHERE user=(?);
            """,
            extractor,
            user.id)
    }
}