package coverosR3z.persistence

import coverosR3z.domainobjects.*
import coverosR3z.exceptions.UserNotRegisteredException
import kotlinx.serialization.Serializable

/**
 * Why use those heavy-handed database applications when you
 * can simply store your data in simple collections?
 *
 * Here, things are simple.  Anything you need, you make.
 */
@Serializable
class PureMemoryDatabase {

    private val users : MutableSet<User> = mutableSetOf()
    private val projects : MutableSet<Project> = mutableSetOf()
    private val timeEntries : MutableMap<User, MutableSet<TimeEntry>> = mutableMapOf()

    fun addTimeEntry(timeEntry : TimeEntryPreDatabase) {
        var userTimeEntries = timeEntries[timeEntry.user]
        if (userTimeEntries == null) {
            userTimeEntries = mutableSetOf()
            timeEntries[timeEntry.user] = userTimeEntries
        }
        val newIndex = userTimeEntries.size + 1
        userTimeEntries.add(TimeEntry(
                newIndex,
                timeEntry.user,
                timeEntry.project,
                timeEntry.time,
                timeEntry.date,
                timeEntry.details))
    }

    fun addNewProject(projectName: ProjectName) : Int {
        val newIndex = projects.size + 1
        projects.add(Project(newIndex, projectName.value))
        return newIndex
    }

    fun addNewUser(username: UserName) : Int {
        val newIndex = users.size + 1
        users.add(User(newIndex, username.value))
        return newIndex
    }

    /**
     * gets the number of minutes a particular [User] has worked
     * on a certain date.
     *
     * @throws [UserNotRegisteredException] if the user isn't known.
     */
    fun getMinutesRecordedOnDate(user: User, date: Date): Int {
        val userTimeEntries = timeEntries[user]
                ?: if (!users.contains(user)) {
                    throw UserNotRegisteredException()
                } else {
                    return 0
                }
        return userTimeEntries
                .filter { te -> te.user.id == user.id && te.date == date }
                .sumBy { te -> te.time.numberOfMinutes }
    }

    fun getAllTimeEntriesForUser(user: User): List<TimeEntry> {
        return timeEntries[user]!!.filter{te -> te.user.id == user.id}
    }

    fun getAllTimeEntriesForUserOnDate(user: User, date: Date): List<TimeEntry> {
        return timeEntries[user]!!.filter{te -> te.user.id == user.id && te.date == date}
    }

    fun getProjectById(id: Int) : Project? {
        assert(id > 0)
        return projects.singleOrNull { p -> p.id == id }
    }

    fun getUserById(id: Int): User? {
        assert(id > 0)
        return users.singleOrNull {u -> u.id == id}
    }

    fun getAllUsers() : List<User> {
        return users.toList()
    }

    fun getAllProjects(): List<Project>? {
        return projects.toList()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PureMemoryDatabase

        if (users != other.users) return false
        if (projects != other.projects) return false
        if (timeEntries != other.timeEntries) return false

        return true
    }

    override fun hashCode(): Int {
        var result = users.hashCode()
        result = 31 * result + projects.hashCode()
        result = 31 * result + timeEntries.hashCode()
        return result
    }


}