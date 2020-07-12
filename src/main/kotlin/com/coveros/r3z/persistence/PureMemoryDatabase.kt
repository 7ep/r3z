package com.coveros.r3z.persistence

import com.coveros.r3z.domainobjects.*

class PureMemoryDatabase {
    private val users : MutableList<User> = mutableListOf()
    private val projects : MutableList<Project> = mutableListOf()
    private val timeEntries : MutableList<TimeEntry> = mutableListOf()

    fun addTimeEntry(timeEntry : TimeEntry) {
        timeEntries.add(timeEntry)
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
                .filter { te -> te.user == user && te.date == date }
                .sumBy { te -> te.time.numberOfMinutes }
    }

    fun getAllTimeEntriesForUser(user: User): List<TimeEntry> {
        return timeEntries.filter{te -> te.user == user}
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