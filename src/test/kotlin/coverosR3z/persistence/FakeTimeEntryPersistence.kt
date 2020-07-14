package coverosR3z.persistence

import coverosR3z.DEFAULT_PROJECT
import coverosR3z.DEFAULT_USER
import coverosR3z.domainobjects.*
import coverosR3z.timerecording.ITimeEntryPersistence


class FakeTimeEntryPersistence(
        val minutesRecorded: Long = 0L,
        val persistNewTimeEntryBehavior : () -> Unit = {},
        val persistNewProjectBehavior : () -> Project = { DEFAULT_PROJECT }) : ITimeEntryPersistence {


    override fun persistNewTimeEntry(entry: TimeEntryForDatabase) {
        persistNewTimeEntryBehavior()
    }

    override fun persistNewProject(projectName: ProjectName): Project {
        return persistNewProjectBehavior()
    }

    override fun persistNewUser(username: UserName): User {
        return DEFAULT_USER
    }


    override fun queryMinutesRecorded(user: User, date: Date): Long {
        return minutesRecorded
    }

    override fun readTimeEntries(user: User): List<TimeEntryForDatabase>? {
        return listOf()
    }

}