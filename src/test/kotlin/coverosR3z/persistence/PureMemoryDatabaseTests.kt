package coverosR3z.persistence

import coverosR3z.domainobjects.*
import org.junit.Assert.assertTrue
import org.junit.Test

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

//    @Before
//    fun init() {
//        pmd = PureMemoryDatabase()
//    }
//
//    @Test fun `should be able to add a new project`() {
//        pmd.addNewProject(ProjectName("my project"))
//    }
//
//    @Test fun `should be able to add a new user`() {
//        pmd.addNewUser(UserName("some user"))
//    }
//
//    @Test fun `should be able to add a new time entry`() {
//        pmd.addTimeEntry(TimeEntryForDatabase())
//    }
//
//    @Test fun `should be able to get the minutes on a certain date`() {
//        pmd.getMinutesRecordedOnDate()
//    }
//
//    @Test fun `should be able to get all time entries for a user`() {
//        pmd.getAllTimeEntriesForUser()
//    }
//
//    @Test fun `should be able to get a project by id`() {
//        pmd.getProjectById()
//    }
//
//    @Test fun `should be able to get a user by id`() {
//        pmd.getUserById()
//    }
}