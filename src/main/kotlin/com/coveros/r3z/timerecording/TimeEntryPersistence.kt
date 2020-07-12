package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.logging.Logger
import com.coveros.r3z.persistence.ProjectIntegrityViolationException
import com.coveros.r3z.persistence.PureMemoryDatabase
import com.coveros.r3z.persistence.UserIntegrityViolationException

class TimeEntryPersistence(val pmd : PureMemoryDatabase) : ITimeEntryPersistence {

    companion object {
        val log : Logger = Logger()
    }

    override fun persistNewTimeEntry(entry: TimeEntry) {
        log.info("persisting a new timeEntry, $entry")
        isEntryValid(entry)
        pmd.addTimeEntry(entry)
    }

    /**
     * This will throw an exception if the project or user in
     * this timeentry don't exist in the list of projects / users
     */
    private fun isEntryValid(entry: TimeEntry) {
        pmd.getProjectById(entry.project.id) ?: throw ProjectIntegrityViolationException()
        pmd.getUserById(entry.user.id) ?: throw UserIntegrityViolationException()
    }

    override fun persistNewProject(projectName: ProjectName): Project {
        log.info("Recording a new project, ${projectName.value}, to the database")
        val newId = pmd.addNewProject(projectName)
        assert(newId > 0) {"A valid project will receive a positive id"}
        return Project(newId, projectName.value)
    }

    override fun persistNewUser(username: UserName): User {
        log.info("Recording a new user, ${username.value}, to the database")

        val newId = pmd.addNewUser(username)

        assert(newId > 0) {"A valid user will receive a positive id"}
        return User(newId, username.value)
    }

    override fun queryMinutesRecorded(user: User, date: Date): Long {
        val minutes = pmd.getMinutesRecordedOnDate(user, date)
        return minutes.toLong()
    }

    override fun readTimeEntries(user: User): List<TimeEntry>? {
        return pmd.getAllTimeEntriesForUser(user)
    }
}