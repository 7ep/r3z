package coverosR3z.persistence

import coverosR3z.domainobjects.*
import coverosR3z.getTime
import coverosR3z.logging.logInfo
import coverosR3z.timerecording.TimeEntryPersistenceH2
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class H2DatabaseTests {

    lateinit var tepH2 : TimeEntryPersistenceH2

    @Before
    fun init() {

        val dataSource = getMemoryBasedDatabaseConnectionPool()
        val flywayHelper = FlywayHelper(dataSource)
        flywayHelper.cleanDatabase()
        flywayHelper.migrateDatabase()
        tepH2 = TimeEntryPersistenceH2(dbHelper = DbAccessHelper(dataSource))
    }

    @Test
    fun `a 200-person firm should be able to add time entries for 10 years`() {
        // generate the 200 users
        val usernames = listOf("Aaren", "Aarika", "Abagael", "Abagail", "Abbe", "Abbey", "Abbi", "Abbie", "Abby", "Abbye", "Abigael", "Abigail", "Abigale", "Abra", "Ada", "Adah", "Adaline", "Adan", "Adara", "Adda", "Addi", "Addia", "Addie", "Addy", "Adel", "Adela", "Adelaida", "Adelaide", "Adele", "Adelheid", "Adelice", "Adelina", "Adelind", "Adeline", "Adella", "Adelle", "Adena", "Adey", "Adi", "Adiana", "Adina", "Adora", "Adore", "Adoree", "Adorne", "Adrea", "Adria", "Adriaens", "Adrian", "Adriana", "Adriane", "Adrianna", "Adrianne", "Adriena", "Adrienne", "Aeriel", "Aeriela", "Aeriell", "Afton", "Ag", "Agace", "Agata", "Agatha", "Agathe", "Aggi", "Aggie", "Aggy", "Agna", "Agnella", "Agnes", "Agnese", "Agnesse", "Agneta", "Agnola", "Agretha", "Aida", "Aidan", "Aigneis", "Aila", "Aile", "Ailee", "Aileen", "Ailene", "Ailey", "Aili", "Ailina", "Ailis", "Ailsun", "Ailyn", "Aime", "Aimee", "Aimil", "Aindrea", "Ainslee", "Ainsley", "Ainslie", "Ajay", "Alaine", "Alameda", "Alana", "Alanah", "Alane", "Alanna", "Alayne", "Alberta", "Albertina", "Albertine", "Albina", "Alecia", "Aleda", "Aleece", "Aleen", "Alejandra", "Alejandrina", "Alena", "Alene", "Alessandra", "Aleta", "Alethea", "Alex", "Alexa", "Alexandra", "Alexandrina", "Alexi", "Alexia", "Alexina", "Alexine", "Alexis", "Alfi", "Alfie", "Alfreda", "Alfy", "Ali", "Alia", "Alica", "Alice", "Alicea", "Alicia", "Alida", "Alidia", "Alie", "Alika", "Alikee", "Alina", "Aline", "Alis", "Alisa", "Alisha", "Alison", "Alissa", "Alisun", "Alix", "Aliza", "Alla", "Alleen", "Allegra", "Allene", "Alli", "Allianora", "Allie", "Allina", "Allis", "Allison", "Allissa", "Allix", "Allsun", "Allx", "Ally", "Allyce", "Allyn", "Allys", "Allyson", "Alma", "Almeda", "Almeria", "Almeta", "Almira", "Almire", "Aloise", "Aloisia", "Aloysia", "Alta", "Althea", "Alvera", "Alverta", "Alvina", "Alvinia", "Alvira", "Alyce", "Alyda", "Alys", "Alysa", "Alyse", "Alysia", "Alyson", "Alyss", "Alyssa", "Amabel", "Amabelle", "Amalea")
        val timeToEnterUsers = getTime {
            usernames.forEach { u -> tepH2.persistNewUser(UserName(u)) }
        }
        logInfo("It took $timeToEnterUsers milliseconds to enter the users")

        lateinit var allUsers : List<User>
        val timeToReadAllUsers = getTime {
            allUsers = tepH2.getAllUsers()!!
        }
        logInfo("It took $timeToReadAllUsers milliseconds to read all the users")

        // generate 2000 projects
        val startCreatingProjects = System.currentTimeMillis()
        (0..2000).forEach{ i -> tepH2.persistNewProject(ProjectName("project$i"))}
        logInfo("It took ${System.currentTimeMillis() - startCreatingProjects} milliseconds to create the projects")

        val startReadingProjects = System.currentTimeMillis()
        val allProjects = tepH2.getAllProjects().orEmpty()
        logInfo("It took ${System.currentTimeMillis() - startReadingProjects} milliseconds to read all the projects")


        val tenYears = 10 * 365
        val startCreatingTimeEntries = System.currentTimeMillis()
        for (day in 1..tenYears) {
            for (user in allUsers) {
                tepH2.persistNewTimeEntry(TimeEntryPreDatabase(user, allProjects.random(), Time(2 * 60), Date.makeDateFromEpoch(18438L + day), Details("a".repeat(500))))
                tepH2.persistNewTimeEntry(TimeEntryPreDatabase(user, allProjects.random(), Time(2 * 60), Date.makeDateFromEpoch(18438L + day), Details("a".repeat(500))))
                tepH2.persistNewTimeEntry(TimeEntryPreDatabase(user, allProjects.random(), Time(2 * 60), Date.makeDateFromEpoch(18438L + day), Details("a".repeat(500))))
                tepH2.persistNewTimeEntry(TimeEntryPreDatabase(user, allProjects.random(), Time(2 * 60), Date.makeDateFromEpoch(18438L + day), Details("a".repeat(500))))
            }
        }
        logInfo("It took ${System.currentTimeMillis() - startCreatingTimeEntries} milliseconds to enter all the time entries")

        val startGettingAllTimeEntriesForUsers = System.currentTimeMillis()
        tepH2.readTimeEntries(allUsers[0])
        logInfo("It took ${System.currentTimeMillis() - startGettingAllTimeEntriesForUsers} milliseconds to get all the time entries for a user")

        val startAccumulateTotalMinutesPerUser = System.currentTimeMillis()
        val minutesPerUserTotal = allUsers.map { u -> tepH2.readTimeEntries(u)!!.sumBy { te -> te.time.numberOfMinutes } }.toList()
        logInfo("the time ${allUsers[0]} spent was ${minutesPerUserTotal[0]}")
        logInfo("the time ${allUsers[1]} spent was ${minutesPerUserTotal[1]}")
        logInfo("It took ${System.currentTimeMillis() - startAccumulateTotalMinutesPerUser} milliseconds to accumulate the minutes per user")

    }
}