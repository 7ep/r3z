package coverosR3z.timerecording

import coverosR3z.domainobjects.*
import coverosR3z.persistence.DbAccessHelper
import java.sql.ResultSet

class TimeEntryPersistenceH2(private val dbHelper: DbAccessHelper) {

    fun persistNewTimeEntry(entry: TimeEntryPreDatabase): Long {
//        logInfo("persisting a new timeEntry, $entry")
        return dbHelper.executeUpdate(
            "INSERT INTO TIMEANDEXPENSES.TIMEENTRY (user, project, time_in_minutes, date, details) VALUES (?, ?,?, ?, ?);",
            entry.user.id, entry.project.id, entry.time.numberOfMinutes, entry.date.sqlDate, entry.details.value)
    }

    fun persistNewProject(projectName: ProjectName) : Project {
        assert(projectName.value.isNotEmpty()) {"Project name cannot be empty"}
//        logInfo("Recording a new project, ${projectName.value}, to the database")

        val id = dbHelper.executeUpdate(
            "INSERT INTO TIMEANDEXPENSES.PROJECT (name) VALUES (?);",
            projectName.value).toInt()

        assert(id > 0) {"A valid project will receive a positive id"}
        return Project(id, projectName.value)
    }

    fun getAllProjects(): List<Project>? {
        return dbHelper.runQuery("SELECT * FROM TIMEANDEXPENSES.PROJECT", {r ->
            val myList: MutableList<Project> = mutableListOf()
            do {
                myList.add(Project(r.getInt("id"), r.getString("name")))
            } while (r.next())
            myList.toList()
        })
    }

    fun persistNewUser(username: UserName): User {
        assert(username.value.isNotEmpty())
//        logInfo("Recording a new user, $username, to the database")

        val newId = dbHelper.executeUpdate(
            "INSERT INTO TIMEANDEXPENSES.USER (name) VALUES (?);", username.value).toInt()

        assert(newId > 0) {"A valid user will receive a positive id"}
        return User(newId, username.value)
    }

    fun getAllUsers(): List<User>? {
        return dbHelper.runQuery("SELECT * FROM TIMEANDEXPENSES.USER", {r ->
            val myList: MutableList<User> = mutableListOf()
            do {
                myList.add(User(r.getInt("id"), r.getString("name")))
            } while (r.next())
            myList.toList()
        })
    }

    /**
     * Provided a user and date, give the number of minutes they worked on that date
     */
    fun queryMinutesRecorded(user: User, date: Date): Long? {
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
                        r.getInt("id"),
                        User(r.getInt("userid"), r.getString("username")),
                        Project(r.getInt("projectid"), r.getString("projectname")),
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
        SELECT te.id, te.user as userid, u.name as username, te.project as projectid, pj.name as projectname, 
            te.time_in_minutes as time, te.date, te.details 
                FROM TIMEANDEXPENSES.TIMEENTRY as te 
                JOIN timeandexpenses.USER as u on te.user = u.id
                JOIN timeandexpenses.PROJECT as pj on te.project = pj.id
                WHERE user=(?);
            """,
            extractor,
            user.id)
    }

}
