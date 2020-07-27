package coverosR3z.timerecording

import coverosR3z.DEFAULT_PROJECT
import coverosR3z.DEFAULT_USER
import coverosR3z.domainobjects.*
import coverosR3z.timerecording.ITimeEntryPersistence


class FakeTimeEntryPersistence(
        var minutesRecorded : Int = 0,
        var persistNewTimeEntryBehavior : () -> Unit = {},
        var persistNewProjectBehavior : () -> Project = { DEFAULT_PROJECT }) : ITimeEntryPersistence {


    override fun persistNewTimeEntry(entry: TimeEntryPreDatabase) {
        persistNewTimeEntryBehavior()
    }

    override fun persistNewProject(projectName: ProjectName): Project {
        return persistNewProjectBehavior()
    }

    override fun persistNewUser(username: UserName): User {
        return DEFAULT_USER
    }


    override fun queryMinutesRecorded(user: User, date: Date): Int {
        return minutesRecorded
    }

    override fun readTimeEntries(user: User): List<TimeEntry> {
        return listOf()
    }

    override fun readTimeEntriesOnDate(user: User, date: Date): List<TimeEntry> {
        return listOf()
    }

}