package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.persistence.microorm.DbAccessHelper
import com.coveros.r3z.persistence.microorm.PureMemoryDatabase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.ResultSet

class TimeEntryPersistence(private val dbHelper: DbAccessHelper) {

    companion object {
        val log : Logger = LoggerFactory.getLogger(TimeEntryPersistence::class.java)
        val pmd = PureMemoryDatabase
    }

    fun persistNewTimeEntry(entry: TimeEntry): Long {
        log.info("persisting a new timeEntry, $entry")

        pmd.addTimeEntry(entry)

        return dbHelper.executeUpdate(
                "INSERT INTO TIMEANDEXPENSES.TIMEENTRY (user, project, time_in_minutes, date, details) VALUES (?, ?,?, ?, ?);",
                entry.user.id, entry.project.id, entry.time.numberOfMinutes, entry.date.sqlDate, entry.details.value)
    }

    fun persistNewProject(projectName: ProjectName) : Project {
        assert(projectName.value.isNotEmpty()) {"Project name cannot be empty"}
        log.info("Recording a new project, ${projectName.value}, to the database")

        val pmdId = pmd.addNewProject(projectName)

        val id = dbHelper.executeUpdate(
                "INSERT INTO TIMEANDEXPENSES.PROJECT (name) VALUES (?);",
                projectName.value)

        assert(id > 0) {"A valid project will receive a positive id"}
        assert(pmdId == id) {"newId, $pmdId should be equal to id, $id"}
        return Project(id, projectName.value)
    }

    fun persistNewUser(username: UserName): User {
        log.info("Recording a new user, ${username.value}, to the database")

        val pmdId = pmd.addNewUser(username)

        val newId = dbHelper.executeUpdate(
            "INSERT INTO TIMEANDEXPENSES.PERSON (name) VALUES (?);", username.value)

        assert(pmdId == newId) {"pmdId, $pmdId should be equal to newId, $newId"}
        assert(newId > 0) {"A valid user will receive a positive id"}
        return User(newId, username.value)
    }

    /**
     * Provided a user and date, give the number of minutes they worked on that date
     */
    fun queryMinutesRecorded(user: User, date: Date): Long? {
        val minutes : Long? = pmd.getMinutesRecordedOnDate(user, date)

        return dbHelper.runQuery(
                "SELECT SUM (TIME_IN_MINUTES) AS total FROM TIMEANDEXPENSES.TIMEENTRY WHERE user=(?) AND date=(?);",
                { r : ResultSet -> r.getLong("total")},
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
        return dbHelper.runQuery(
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