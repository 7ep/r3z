package com.coveros.r3z.persistence.microorm

import com.coveros.r3z.domainobjects.Project
import com.coveros.r3z.domainobjects.ProjectName
import com.coveros.r3z.domainobjects.TimeEntry
import com.coveros.r3z.domainobjects.User

object PureMemoryDatabase {
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

    fun clearDatabase() {
        users.clear()
        projects.clear()
        timeEntries.clear()
    }
}