package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.persistence.microorm.PureMemoryDatabase
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TimeEntryPersistence2(val pmd : PureMemoryDatabase) : ITimeEntryPersistence {

    companion object {
        val log : Logger = LoggerFactory.getLogger(TimeEntryPersistence2::class.java)
    }

    override fun persistNewTimeEntry(entry: TimeEntry) {
        log.info("persisting a new timeEntry, $entry")

        pmd.addTimeEntry(entry)
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
        TODO("Not yet implemented")
    }
}