package coverosR3z.timerecording

import coverosR3z.domainobjects.*

interface ITimeEntryPersistence {
    fun persistNewTimeEntry(entry: TimeEntry)
    fun persistNewProject(projectName: ProjectName) : Project
    fun persistNewUser(username: UserName): User

    /**
     * Provided a user and date, give the number of minutes they worked on that date
     */
    fun queryMinutesRecorded(user: User, date: Date): Long
    fun readTimeEntries(user: User): List<TimeEntry>?

}