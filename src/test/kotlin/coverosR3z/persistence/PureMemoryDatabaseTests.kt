package coverosR3z.persistence

import coverosR3z.*
import coverosR3z.domainobjects.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlin.random.nextUInt

class PureMemoryDatabaseTests {

    lateinit var pmd : PureMemoryDatabase

    @Before
    fun init() {
        pmd = PureMemoryDatabase()
    }

    @Test fun `should be able to add a new project`() {
        pmd.addNewProject(DEFAULT_PROJECT_NAME)

        val project = pmd.getProjectById(DEFAULT_PROJECT.id)

        assertEquals(1, project!!.id)
    }

    @Test fun `should be able to add a new user`() {
        pmd.addNewUser(DEFAULT_USERNAME)

        val user = pmd.getUserById(DEFAULT_USER.id)

        assertEquals(1, user!!.id)
    }

    @Test fun `should be able to add a new time entry`() {
        pmd.addTimeEntry(TimeEntryPreDatabase(DEFAULT_USER, DEFAULT_PROJECT, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020))

        val timeEntries = pmd.getAllTimeEntriesForUser(DEFAULT_USER)[0]

        assertEquals(1, timeEntries.id)
        assertEquals(DEFAULT_USER, timeEntries.user)
        assertEquals(DEFAULT_PROJECT, timeEntries.project)
        assertEquals(DEFAULT_TIME, timeEntries.time)
        assertEquals(A_RANDOM_DAY_IN_JUNE_2020, timeEntries.date)
    }

    @Test fun `a 200-person firm should be able to add time entries for 10 years`() {
        // generate the 200 users
        for (p in 0..200) {
//            var user = pmd.addNewUser(UserName("Testfolk"))
            var user = User(p, "Testfolk")
            pmd.addNewUser(UserName(user.name))
            // create a user
            // create a random description
            // create 4 entries per day for them, 5 days a week
            var months = listOf(Month.JAN, Month.FEB, Month.MAR, Month.APR, Month.MAY, Month.JUN, Month.JUL,
                Month.AUG, Month.SEP, Month.OCT, Month.NOV, Month.DEC)
            for(y in 2020..2021) {
                for (m in months) {
                    for (d in 1..22) {
                        repeat(4) {
                            val time = Time(Random.nextInt(60, 120))
                            val date = Date(y, m, d)
                            val garble = "abcdefghijklmnopqrstuvwxyz"[Random.nextInt(0, 26)].toString().repeat(Random.nextInt(0, 500))
                            val details = Details(garble)
                            val entry = TimeEntryPreDatabase(user, DEFAULT_PROJECT, time, date, details)
                            pmd.addTimeEntry(entry)
                        }
                    }
                }
            }
        }
        assertEquals("", pmd.getMinutesRecordedOnDate(User(1, "testfolk"), Date(2020, Month.JAN, 1)))
        assertEquals("", pmd.getAllTimeEntriesForUser(User(1, "testfolk")))

        // generate 2000 projects

        // add time entries for those users,
        // adding 4 entries per day and random text between 0 and 500 chars in details
        // for 5 days a week, for 10 years

//        pmd.addTimeEntry(Time)

        // we should be able to get the entries for years ago on a given day right fast

        // we should be able to run a cumulative report on data for 10 years right fast on a given user
    }

    @Test fun `should be able to get the minutes on a certain date`() {
        pmd.addTimeEntry(TimeEntryPreDatabase(DEFAULT_USER, DEFAULT_PROJECT, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020))

        val minutes = pmd.getMinutesRecordedOnDate(DEFAULT_USER, A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals(DEFAULT_TIME.numberOfMinutes, minutes)
    }

}