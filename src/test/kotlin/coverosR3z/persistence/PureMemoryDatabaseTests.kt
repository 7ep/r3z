package coverosR3z.persistence

import coverosR3z.*
import coverosR3z.domainobjects.*
import coverosR3z.logging.logInfo
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class PureMemoryDatabaseTests {

    lateinit var pmd: PureMemoryDatabase

    @Before
    fun init() {
        pmd = PureMemoryDatabase()
    }

    @Test
    fun `should be able to add a new project`() {
        pmd.addNewProject(DEFAULT_PROJECT_NAME)

        val project = pmd.getProjectById(DEFAULT_PROJECT.id)

        assertEquals(1, project!!.id)
    }

    @Test
    fun `should be able to add a new user`() {
        pmd.addNewUser(DEFAULT_USERNAME)

        val user = pmd.getUserById(DEFAULT_USER.id)

        assertEquals(1, user!!.id)
    }

    @Test
    fun `should be able to add a new time entry`() {
        pmd.addTimeEntry(TimeEntryPreDatabase(DEFAULT_USER, DEFAULT_PROJECT, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020))

        val timeEntries = pmd.getAllTimeEntriesForUser(DEFAULT_USER)[0]

        assertEquals(1, timeEntries.id)
        assertEquals(DEFAULT_USER, timeEntries.user)
        assertEquals(DEFAULT_PROJECT, timeEntries.project)
        assertEquals(DEFAULT_TIME, timeEntries.time)
        assertEquals(A_RANDOM_DAY_IN_JUNE_2020, timeEntries.date)
    }

    @Ignore("This is not for common regular testing.  It is an artifact of the database design process")
    @Test
    fun `a 200-person firm should be able to add time entries for 10 years`() {
        // generate the 200 users
        val usernames = listOf("Aaren", "Aarika", "Abagael", "Abagail", "Abbe", "Abbey", "Abbi", "Abbie", "Abby", "Abbye", "Abigael", "Abigail", "Abigale", "Abra", "Ada", "Adah", "Adaline", "Adan", "Adara", "Adda", "Addi", "Addia", "Addie", "Addy", "Adel", "Adela", "Adelaida", "Adelaide", "Adele", "Adelheid", "Adelice", "Adelina", "Adelind", "Adeline", "Adella", "Adelle", "Adena", "Adey", "Adi", "Adiana", "Adina", "Adora", "Adore", "Adoree", "Adorne", "Adrea", "Adria", "Adriaens", "Adrian", "Adriana", "Adriane", "Adrianna", "Adrianne", "Adriena", "Adrienne", "Aeriel", "Aeriela", "Aeriell", "Afton", "Ag", "Agace", "Agata", "Agatha", "Agathe", "Aggi", "Aggie", "Aggy", "Agna", "Agnella", "Agnes", "Agnese", "Agnesse", "Agneta", "Agnola", "Agretha", "Aida", "Aidan", "Aigneis", "Aila", "Aile", "Ailee", "Aileen", "Ailene", "Ailey", "Aili", "Ailina", "Ailis", "Ailsun", "Ailyn", "Aime", "Aimee", "Aimil", "Aindrea", "Ainslee", "Ainsley", "Ainslie", "Ajay", "Alaine", "Alameda", "Alana", "Alanah", "Alane", "Alanna", "Alayne", "Alberta", "Albertina", "Albertine", "Albina", "Alecia", "Aleda", "Aleece", "Aleen", "Alejandra", "Alejandrina", "Alena", "Alene", "Alessandra", "Aleta", "Alethea", "Alex", "Alexa", "Alexandra", "Alexandrina", "Alexi", "Alexia", "Alexina", "Alexine", "Alexis", "Alfi", "Alfie", "Alfreda", "Alfy", "Ali", "Alia", "Alica", "Alice", "Alicea", "Alicia", "Alida", "Alidia", "Alie", "Alika", "Alikee", "Alina", "Aline", "Alis", "Alisa", "Alisha", "Alison", "Alissa", "Alisun", "Alix", "Aliza", "Alla", "Alleen", "Allegra", "Allene", "Alli", "Allianora", "Allie", "Allina", "Allis", "Allison", "Allissa", "Allix", "Allsun", "Allx", "Ally", "Allyce", "Allyn", "Allys", "Allyson", "Alma", "Almeda", "Almeria", "Almeta", "Almira", "Almire", "Aloise", "Aloisia", "Aloysia", "Alta", "Althea", "Alvera", "Alverta", "Alvina", "Alvinia", "Alvira", "Alyce", "Alyda", "Alys", "Alysa", "Alyse", "Alysia", "Alyson", "Alyss", "Alyssa", "Amabel", "Amabelle", "Amalea")
        val timeToEnterUsers = getTime {
            usernames.forEach { u -> pmd.addNewUser(UserName(u)) }
        }
        logInfo("It took $timeToEnterUsers milliseconds to enter the users")

        lateinit var allUsers: List<User>
        val timeToReadAllUsers = getTime {
            allUsers = pmd.getAllUsers()!!
        }
        logInfo("It took $timeToReadAllUsers milliseconds to read all the users")

        // generate 2000 projects
        val timeToCreateProjects = getTime { (0..2000).forEach { i -> pmd.addNewProject(ProjectName("project$i")) } }
        logInfo("It took $timeToCreateProjects milliseconds to create the projects")

        lateinit var allProjects: List<Project>
        val timeToReadAllProjects = getTime { allProjects = pmd.getAllProjects().orEmpty() }
        logInfo("It took $timeToReadAllProjects milliseconds to read all the projects")

        val tenYears = 10 * 365
        val timeToEnterAllTimeEntries = getTime {
            for (day in 1..tenYears) {
                for (user in allUsers) {
                    pmd.addTimeEntry(TimeEntryPreDatabase(user, allProjects.random(), Time(2 * 60), Date.makeDateFromEpoch(18438L + day), Details("a".repeat(500))))
                    pmd.addTimeEntry(TimeEntryPreDatabase(user, allProjects.random(), Time(2 * 60), Date.makeDateFromEpoch(18438L + day), Details("a".repeat(500))))
                    pmd.addTimeEntry(TimeEntryPreDatabase(user, allProjects.random(), Time(2 * 60), Date.makeDateFromEpoch(18438L + day), Details("a".repeat(500))))
                    pmd.addTimeEntry(TimeEntryPreDatabase(user, allProjects.random(), Time(2 * 60), Date.makeDateFromEpoch(18438L + day), Details("a".repeat(500))))
                }
            }
        }
        logInfo("It took $timeToEnterAllTimeEntries milliseconds to enter all the time entries")

        val timeToGetAllTimeEntries = getTime { pmd.getAllTimeEntriesForUser(allUsers[0]) }
        logInfo("It took $timeToGetAllTimeEntries milliseconds to get all the time entries for a user")

        val timeToAccumulate = getTime {
            val minutesPerUserTotal = allUsers.map { u -> pmd.getAllTimeEntriesForUser(u).sumBy { te -> te.time.numberOfMinutes } }.toList()
            logInfo("the time ${allUsers[0]} spent was ${minutesPerUserTotal[0]}")
            logInfo("the time ${allUsers[1]} spent was ${minutesPerUserTotal[1]}")
        }

        logInfo("It took $timeToAccumulate milliseconds to accumulate the minutes per user")

        // we should be able to get the entries for years ago on a given day right fast

        // we should be able to run a cumulative report on data for 10 years right fast on a given user
    }

    @Test
    fun `should be able to get the minutes on a certain date`() {
        pmd.addTimeEntry(TimeEntryPreDatabase(DEFAULT_USER, DEFAULT_PROJECT, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020))

        val minutes = pmd.getMinutesRecordedOnDate(DEFAULT_USER, A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals(DEFAULT_TIME.numberOfMinutes, minutes)
    }

}