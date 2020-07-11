package com.coveros.r3z.persistence.microorm

import com.coveros.r3z.domainobjects.*

class PureMemoryDatabase {
    private val users : MutableList<User> = mutableListOf()
    private val projects : MutableList<Project> = mutableListOf()
    private val timeEntries : MutableList<TimeEntry> = mutableListOf()

    fun addTimeEntry(timeEntry : TimeEntry) {
        timeEntries.add(timeEntry)
    }

    fun getTimeEntryByUser(user : User) : Iterable<TimeEntry> {
        return timeEntries.filter { te -> te.user.id == user.id}
    }

    fun addNewProject(projectName: ProjectName) : Long {
        val newIndex = projects.size.toLong() + 1
        projects.add(Project(newIndex, projectName.value))
        return newIndex
    }

    fun addNewUser(username: UserName) : Long {
        val newIndex = users.size.toLong() + 1
        users.add(User(newIndex, username.value))
        return newIndex
    }

    fun getMinutesRecordedOnDate(user: User, date: Date): Int {
        return timeEntries
                .filter { te -> te.user == user && te.date == date }
                .sumBy { te -> te.time.numberOfMinutes }
    }

    fun clearDatabase() {
        users.clear()
        projects.clear()
        timeEntries.clear()
    }

}