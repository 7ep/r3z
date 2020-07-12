package com.coveros.r3z.timerecording

import com.coveros.r3z.A_RANDOM_DAY_IN_JUNE_2020
import com.coveros.r3z.A_RANDOM_DAY_IN_JUNE_2020_PLUS_ONE
import com.coveros.r3z.DEFAULT_USER
import com.coveros.r3z.createTimeEntry
import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.persistence.ProjectIntegrityViolationException
import com.coveros.r3z.persistence.PureMemoryDatabase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TimeEntryPersistenceTests {

    private var tep : ITimeEntryPersistence = TimeEntryPersistence2(PureMemoryDatabase())

    @Before fun init() {
        tep = TimeEntryPersistence2(PureMemoryDatabase())
    }

    @Test fun `can record a time entry to the database`() {
        val newProject = tep.persistNewProject(ProjectName("test project"))
        tep.persistNewTimeEntry(createTimeEntry(project = newProject))
        val count = tep.readTimeEntries(DEFAULT_USER)!!.size
        assertEquals("There should be exactly one entry in the database", 1, count)
    }

    @Test fun `can get all time entries by a user`() {
        val userName = UserName("test")
        val user = User(1, userName.value)
        tep.persistNewUser(userName)
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val entry1 = createTimeEntry(user = user, project = newProject)
        val entry2 = createTimeEntry(user = user, project = newProject)
        tep.persistNewTimeEntry(entry1)
        tep.persistNewTimeEntry(entry2)
        val expectedResult = listOf(entry1, entry2)

        val actualResult = tep.readTimeEntries(user) ?: listOf()

        val msg = "what we entered and what we get back should be identical, instead got"
        assertEquals(msg, expectedResult, actualResult)
    }

    /**
     * If we try to add a time entry with a project id that doesn't exist in
     * the database, we should get an exception back from the database
     */
    @Test fun `Can't record a time entry that has a nonexistent project id`() {
        assertThrows(ProjectIntegrityViolationException::class.java) {
            tep.persistNewTimeEntry(createTimeEntry())
        }
    }

    /**
     * We need to be able to know how many hours a user has worked for the purpose of validation
     */
    @Test
    fun `Can query hours worked by a user on a given day`() {
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val testUser = User(1, "test")
        tep.persistNewTimeEntry(
            createTimeEntry(
                user = testUser,
                time = Time(60),
                project = newProject,
                date = A_RANDOM_DAY_IN_JUNE_2020
            )
        )

        val query = tep.queryMinutesRecorded(user=testUser, date= A_RANDOM_DAY_IN_JUNE_2020)
        assertEquals(60L, query)
    }

    @Test
    fun `if a user has not worked on a given day, we return 0 as their minutes worked that day`() {
        val testUser = User(1, "test")

        val minutesWorked = tep.queryMinutesRecorded(user=testUser, date= A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals("should be 0 since they didn't work that day", 0L, minutesWorked)
    }

    @Test
    fun `If a user worked 24 hours total in a day, we should get that from queryMinutesRecorded`() {
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val newUser = tep.persistNewUser(UserName("test user"))
        tep.persistNewTimeEntry(
            createTimeEntry(
                user = newUser,
                time = Time(60),
                project = newProject,
                date = A_RANDOM_DAY_IN_JUNE_2020
            )
        )
        tep.persistNewTimeEntry(
            createTimeEntry(
                user = newUser,
                time = Time(60 * 10),
                project = newProject,
                date = A_RANDOM_DAY_IN_JUNE_2020
            )
        )
        tep.persistNewTimeEntry(
            createTimeEntry(
                user = newUser,
                time = Time(60 * 13),
                project = newProject,
                date = A_RANDOM_DAY_IN_JUNE_2020
            )
        )

        val query = tep.queryMinutesRecorded(user=newUser, date= A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals("we should get 24 hours worked for this day", 60L * 24, query)
    }


    @Test
    fun `If a user worked 8 hours a day for two days, we should get just 8 hours when checking one of those days`() {
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val testUser = User(1, "test")
        tep.persistNewTimeEntry(
            createTimeEntry(
                user = testUser,
                time = Time(60 * 8),
                project = newProject,
                date = A_RANDOM_DAY_IN_JUNE_2020
            )
        )
        tep.persistNewTimeEntry(
            createTimeEntry(
                user = testUser,
                time = Time(60 * 8),
                project = newProject,
                date = A_RANDOM_DAY_IN_JUNE_2020_PLUS_ONE
            )
        )

        val query = tep.queryMinutesRecorded(user=testUser, date= A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals("we should get 8 hours worked for this day", 60L * 8, query)
    }

}