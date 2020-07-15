package coverosR3z.persistence

import coverosR3z.domainobjects.*

/**
 * Why use those heavy-handed database applications when you
 * can simply store your data in simple collections?
 *
 * Here, things are simple.  Anything you need, you make.
 */
class PureMemoryDatabase {

    private val users : MutableSet<User> = mutableSetOf()
    private val projects : MutableSet<Project> = mutableSetOf()
    private val timeEntries : MutableSet<TimeEntry> = mutableSetOf()

    fun addTimeEntry(timeEntry : TimeEntryPreDatabase) {
        val newIndex = timeEntries.size + 1
        timeEntries.add(TimeEntry(
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

    fun getMinutesRecordedOnDate(user: User, date: Date): Int {
        return timeEntries
                .filter { te -> te.user.id == user.id && te.date == date }
                .sumBy { te -> te.time.numberOfMinutes }
    }

    fun getAllTimeEntriesForUser(user: User): List<TimeEntry> {
        return timeEntries.filter{te -> te.user.id == user.id}
    }

    fun getProjectById(id: Int) : Project? {
        assert(id > 0)
        return projects.singleOrNull { p -> p.id == id }
    }

    fun getUserById(id: Int): User? {
        assert(id > 0)
        return users.singleOrNull {u -> u.id == id}
    }

}