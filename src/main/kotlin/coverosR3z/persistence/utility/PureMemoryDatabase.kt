package coverosR3z.persistence.utility

import coverosR3z.authentication.types.Session
import coverosR3z.authentication.types.User
import coverosR3z.config.CURRENT_DATABASE_VERSION
import coverosR3z.logging.logImperative
import coverosR3z.logging.logWarn
import coverosR3z.misc.utility.ActionQueue
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.persistence.exceptions.NoTimeEntriesOnDiskException
import coverosR3z.persistence.types.*
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*
import java.io.File
import java.io.FileNotFoundException
import kotlin.NoSuchElementException


/**
 * Why use those heavy-handed database applications when you
 * can simply store your data in simple collections?
 *
 * Here, things are simple.  Anything you need, you make.
 *
 * @param dbDirectory if this is null, the database won't use the disk at all.  If you set it to a directory, like
 *                      File("db/") the database will use that directory for all persistence.
 */
open class PureMemoryDatabase(protected val employees: ChangeTrackingSet<Employee> = ChangeTrackingSet(),
                         protected val users: ChangeTrackingSet<User> = ChangeTrackingSet(),
                         protected val projects: ChangeTrackingSet<Project> = ChangeTrackingSet(),
                         protected val timeEntries: ChangeTrackingSet<TimeEntry> = ChangeTrackingSet(),
                         protected val sessions: ChangeTrackingSet<Session> = ChangeTrackingSet(),
                         protected val dbDirectory : String? = null
) {

    private val actionQueue = ActionQueue("DatabaseWriter")
    private val diskPersistence = DatabaseDiskPersistence(dbDirectory)

    fun copy(): PureMemoryDatabase {
        return PureMemoryDatabase(
            employees = this.employees.toList().toChangeTrackingSet(),
            users = this.users.toList().toChangeTrackingSet(),
            projects = this.projects.toList().toChangeTrackingSet(),
            timeEntries = this.timeEntries.toList().toChangeTrackingSet(),
            sessions = this.sessions.toList().toChangeTrackingSet(),
        )
    }

    ////////////////////////////////////
    //   DATA ACCESS
    ////////////////////////////////////

    inner class EmployeeDataAccess : AbstractDataAccess<Employee>(employees, diskPersistence, EMPLOYEES)
    inner class ProjectDataAccess : AbstractDataAccess<Project>(projects, diskPersistence, PROJECTS)
    inner class UserDataAccess : AbstractDataAccess<User>(users, diskPersistence, USERS)
    inner class SessionDataAccess : AbstractDataAccess<Session>(sessions, diskPersistence, SESSIONS)
    inner class TimeEntryDataAccess : AbstractDataAccess<TimeEntry>(timeEntries, diskPersistence, TIME_ENTRIES)


    ////////////////////////////////////
    //   BOILERPLATE
    ////////////////////////////////////



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PureMemoryDatabase

        if (employees != other.employees) return false
        if (users != other.users) return false
        if (projects != other.projects) return false
        if (timeEntries != other.timeEntries) return false
        if (sessions != other.sessions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = employees.hashCode()
        result = 31 * result + users.hashCode()
        result = 31 * result + projects.hashCode()
        result = 31 * result + timeEntries.hashCode()
        result = 31 * result + sessions.hashCode()
        return result
    }


    ////////////////////////////////////
    //   DATABASE CONTROL
    ////////////////////////////////////


    /**
     * This function will stop the database cleanly.
     *
     * In order to do this, we need to wait for our threads
     * to finish their work.  In particular, we
     * have offloaded our file writes to [actionQueue], which
     * has an internal thread for serializing all actions
     * on our database
     */
    fun stop() {
        diskPersistence.stop()
    }

    companion object {

        const val EMPLOYEES = "employees"
        const val SESSIONS = "sessions"
        const val PROJECTS = "projects"
        const val USERS = "users"
        const val TIME_ENTRIES = "time_entries"

        /**
         * This starts the database with memory-only, that is
         * no disk persistence.  This is mainly
         * used for testing purposes.  For production use,
         * check out [DatabaseDiskPersistence.startWithDiskPersistence]
         */
        fun startMemoryOnly() : PureMemoryDatabase {
            val pmd = PureMemoryDatabase()

            logImperative("creating an initial employee")
            val tep = TimeEntryPersistence(pmd)
            tep.persistNewEmployee(EmployeeName("Administrator"))

            return pmd
        }
    }
}