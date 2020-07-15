package coverosR3z.persistence

import coverosR3z.*
import coverosR3z.domainobjects.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.minutes

class PureMemoryDatabaseTests {

    lateinit var pmd : PureMemoryDatabase

    @Test fun createStuff() {
        val user = User(1, "my name is hello")
        val project = Project(1, "I am a good little company")
        val tes = mutableListOf<TimeEntry>()
        tes.add(TimeEntry(1, user, project, Time(123), Date(2020, Month.DEC, 6)))
        tes.add(TimeEntry(2, user, project, Time(22),  Date(2020, Month.DEC, 6)))
        assertTrue(tes[0].user === user)
        assertTrue(tes[0].project === project)
        assertTrue(tes[1].user === user)
        assertTrue(tes[1].project === project)
    }

    @Before
    fun init() {
        pmd = PureMemoryDatabase()
    }

    @Test fun `should be able to add a new project`() {
        pmd.addNewProject(DEFAULT_PROJECTNAME)
        val project = pmd.getProjectById(DEFAULT_PROJECT.id)
        assertEquals(1, project!!.id)
    }

    @Test fun `should be able to add a new user`() {
        pmd.addNewUser(DEFAULT_USERNAME)
        val user = pmd.getUserById(DEFAULT_USER.id)
        assertEquals(1, user!!.id)
    }

    @Test fun `should be able to add a new time entry`() {
        pmd.addTimeEntry(TimeEntry(1, DEFAULT_USER, DEFAULT_PROJECT, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020))
        val timeEntries = pmd.getAllTimeEntriesForUser(DEFAULT_USER)[0]
        assertEquals(1, timeEntries.id)
        assertEquals(DEFAULT_USER, timeEntries.user)
        assertEquals(DEFAULT_PROJECT, timeEntries.project)
        assertEquals(DEFAULT_TIME, timeEntries.time)
        assertEquals(A_RANDOM_DAY_IN_JUNE_2020, timeEntries.date)
    }

    @Test fun `should be able to get the minutes on a certain date`() {
        pmd.addTimeEntry(TimeEntry(1, DEFAULT_USER, DEFAULT_PROJECT, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020))
        val minutes = pmd.getMinutesRecordedOnDate(DEFAULT_USER, A_RANDOM_DAY_IN_JUNE_2020)
        assertEquals(DEFAULT_TIME.numberOfMinutes, minutes)
    }

}