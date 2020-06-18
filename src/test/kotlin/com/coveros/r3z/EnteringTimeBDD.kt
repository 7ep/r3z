package com.coveros.r3z

import com.coveros.r3z.domainobjects.Project
import com.coveros.r3z.domainobjects.Time
import com.coveros.r3z.domainobjects.TimeEntry
import com.coveros.r3z.domainobjects.User
import org.junit.Test


/**
 * As an employee
 * I want to record my time
 * So that I am easily able to document my time in an organized way
 */
class EnteringTimeBDD {
    @Test
    fun `capability to enter time`() {
        // `given I have worked 1 hour on project "A" on Monday`()
        val user = User(1, "I")
        val time = Time(60)
        val project = Project(1, "A")
//        val entry = TimeEntry(user, project, time)

        // `when I enter in that time`()
//        val recordStatus : RecordTimeResult = recordTime(entry)

        // `then the system indicates it has persisted the new information`()
//        assert(recordStatus)
    }

    private fun `when I enter in that time`() {

    }

    private fun `then the system indicates it has persisted the new information`() {
        TODO("Not yet implemented")
    }

}