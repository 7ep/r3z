package coverosR3z.persistence

import coverosR3z.*
import coverosR3z.domainobjects.*
import coverosR3z.logging.logInfo
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

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

    @Test
    fun `a firm should get responses from the database quite quickly`() {
        val numberOfUsers = 30
        val numberOfProjects = 30
        val numberOfDays = 31

        val totalTime = getTime {
            val allUsers = recordManyTimeEntries(numberOfUsers, numberOfProjects, numberOfDays)
            readTimeEntriesForOneUser(allUsers)
            accumulateMinutesPerEachUser(allUsers)
        }

        logInfo("It took a total of $totalTime milliseconds for this code")
    }

    @Test
    fun `should be able to get the minutes on a certain date`() {
        pmd.addTimeEntry(TimeEntryPreDatabase(DEFAULT_USER, DEFAULT_PROJECT, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020))

        val minutes = pmd.getMinutesRecordedOnDate(DEFAULT_USER, A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals(DEFAULT_TIME.numberOfMinutes, minutes)
    }

    /**
     * This is an experimental performance test to see how the system handles
     * serialization, writing to disk, reading from disk, and deserialization
     */
    @Test
    fun `should be possible to quickly serialize our data`() {
        val numberOfUsers = 3
        val numberOfProjects = 6
        val numberOfDays = 5

        recordManyTimeEntries(numberOfUsers, numberOfProjects, numberOfDays)

        val totalTime = getTime {
            val pmdJson = jsonSerialzation.stringify(PureMemoryDatabase.serializer(), pmd)
            val deserializedPmd: PureMemoryDatabase = jsonSerialzation.parse(PureMemoryDatabase.serializer(), pmdJson)
            assertEquals(pmd, deserializedPmd)
        }
        logInfo("Total time taken for serialization / deserialzation was $totalTime milliseconds")
    }

    /**
     * Here we'll try out disk writing / reading with serialization
     */
    @Test
    fun `should be possible to quickly write our data to disk`() {
        val numberOfUsers = 10
        val numberOfProjects = 20
        val numberOfDays = 5

        recordManyTimeEntries(numberOfUsers, numberOfProjects, numberOfDays)

        val totalTime = getTime {
            val pmdJson = jsonSerialzationWithPrettyPrint.stringify(PureMemoryDatabase.serializer(), pmd)
            File("build/tmp/").mkdirs()
            val dbFile = File("build/tmp/pmdrecord.txt")
            dbFile.writeText(pmdJson)
            val readFile = dbFile.readText()
            val deserializedPmd: PureMemoryDatabase = jsonSerialzationWithPrettyPrint.parse(PureMemoryDatabase.serializer(), readFile)
            assertEquals(pmd, deserializedPmd)
        }
        logInfo("Total time taken for serialization / deserialzation was $totalTime milliseconds")
    }

    private fun recordManyTimeEntries(numberOfUsers: Int, numberOfProjects: Int, numberOfDays: Int) : List<User> {
        val lotsOfUsers: List<String> = generateUserNames()
        persistUsersToDatabase(numberOfUsers, lotsOfUsers)
        val allUsers: List<User> = readUsersFromDatabase()
        persistProjectsToDatabase(numberOfProjects)
        val allProjects: List<Project> = readProjectsFromDatabase()
        enterTimeEntries(numberOfDays, allUsers, allProjects, numberOfUsers)
        return allUsers
    }

    private fun accumulateMinutesPerEachUser(allUsers: List<User>) {
        val timeToAccumulate = getTime {
            val minutesPerUserTotal =
                    allUsers.map { u -> pmd.getAllTimeEntriesForUser(u).sumBy { te -> te.time.numberOfMinutes } }
                            .toList()
            logInfo("the time ${allUsers[0]} spent was ${minutesPerUserTotal[0]}")
            logInfo("the time ${allUsers[1]} spent was ${minutesPerUserTotal[1]}")
        }

        logInfo("It took $timeToAccumulate milliseconds to accumulate the minutes per user")
    }

    private fun readTimeEntriesForOneUser(allUsers: List<User>) {
        val timeToGetAllTimeEntries = getTime { pmd.getAllTimeEntriesForUser(allUsers[0]) }
        logInfo("It took $timeToGetAllTimeEntries milliseconds to get all the time entries for a user")
    }

    private fun enterTimeEntries(numberOfDays: Int, allUsers: List<User>, allProjects: List<Project>, numberOfUsers: Int) {
        val timeToEnterAllTimeEntries = getTime {
            for (day in 1..numberOfDays) {
                for (user in allUsers) {
                    pmd.addTimeEntry(TimeEntryPreDatabase(user, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                    pmd.addTimeEntry(TimeEntryPreDatabase(user, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                    pmd.addTimeEntry(TimeEntryPreDatabase(user, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                    pmd.addTimeEntry(TimeEntryPreDatabase(user, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                }
            }
        }
        logInfo("It took $timeToEnterAllTimeEntries milliseconds total to enter ${numberOfDays * 4} time entries for each of $numberOfUsers users")
        logInfo("(That's a total of ${("%,d".format(numberOfDays * 4 * numberOfUsers))} time entries)")
    }

    private fun readProjectsFromDatabase(): List<Project> {
        lateinit var allProjects: List<Project>
        val timeToReadAllProjects = getTime { allProjects = pmd.getAllProjects().orEmpty() }
        logInfo("It took $timeToReadAllProjects milliseconds to read all the projects")
        return allProjects
    }

    private fun persistProjectsToDatabase(numberOfProjects: Int) {
        val timeToCreateProjects =
                getTime { (0..numberOfProjects).forEach { i -> pmd.addNewProject(ProjectName("project$i")) } }
        logInfo("It took $timeToCreateProjects milliseconds to create $numberOfProjects projects")
    }

    private fun readUsersFromDatabase(): List<User> {
        lateinit var allUsers: List<User>
        val timeToReadAllUsers = getTime {
            allUsers = pmd.getAllUsers()!!
        }
        logInfo("It took $timeToReadAllUsers milliseconds to read all the users")
        return allUsers
    }

    private fun persistUsersToDatabase(numberOfUsers: Int, lotsOfUsers: List<String>) {
        val timeToEnterUsers = getTime {
            for (i in 0..numberOfUsers) {
                pmd.addNewUser(UserName(lotsOfUsers[i]))
            }
        }
        logInfo("It took $timeToEnterUsers milliseconds to enter $numberOfUsers users")
    }

    private fun generateUserNames(): List<String> {
        lateinit var lotsOfUsers: List<String>
        val timeToMakeUsernames = getTime {
            val usernames = listOf(
                    "Arlen", "Hedwig", "Allix", "Tandi", "Silvia", "Catherine", "Mavis", "Hally", "Renate", "Anastasia", "Christy", "Nora", "Molly", "Nelli", "Daphna", "Chloette", "TEirtza", "Nannie", "Melinda", "Tyne", "Belva", "Pam", "Rebekkah", "Elayne", "Dianne", "Christina", "Jeanne", "Norry", "Reina", "Erminia", "Eadie", "Valina", "Gayle", "Wylma", "Annette", "Annmaria", "Fayina", "Dita", "Sibella", "Alis", "Georgena", "Luciana", "Sidonnie", "Dina", "Ferdinande", "Coletta", "Esma", "Saidee", "Hannah", "Colette", "Anitra", "Grissel", "Caritta", "Ann", "Rhodia", "Meta", "Bride", "Dalenna", "Rozina", "Ottilie", "Eliza", "Gerda", "Anthia", "Kathryn", "Lilian", "Jeannie", "Nichole", "Marylinda", "Angelica", "Margie", "Ruthie", "Augustina", "Netta", "Fleur", "Mahala", "Cosette", "Zsa Zsa", "Karry", "Tabitha", "Andriana", "Fey", "Hedy", "Saudra", "Geneva", "Lacey", "Fawnia", "Ertha", "Bernie", "Natty", "Joyan", "Teddie", "Hephzibah", "Vonni", "Ambur", "Lyndsie", "Anna", "Minnaminnie", "Andy", "Brina", "Pamella", "Trista", "Antonetta", "Kerrin", "Crysta", "Kira", "Gypsy", "Candy", "Ree", "Sharai", "Mariana", "Eleni", "Yetty", "Maisie", "Deborah", "Doretta", "Juliette", "Esta", "Amandi", "Anallise", "Indira", "Aura", "Melodee", "Desiri", "Jacquenetta", "Joell", "Delcine", "Justine", "Theresita", "Belia", "Mallory", "Antonie", "Jobi", "Katalin", "Kelli", "Ester", "Katey", "Gianna", "Berry", "Sidonia", "Roseanne", "Cherida", "Beatriz", "Eartha", "Robina", "Florri", "Vitoria", "Debera", "Jeanette", "Almire", "Saree", "Liana", "Ruth", "Renell", "Katinka", "Anya", "Gwyn", "Kaycee", "Rori", "Rianon", "Joann", "Zorana", "Hermia", "Gwenni", "Poppy", "Dedie", "Cloe", "Kirsti", "Krysta", "Clarinda", "Enid", "Katina", "Ralina", "Meryl", "Andie", "Orella", "Alexia", "Clarey", "Iris", "Chris", "Devin", "Kally", "Vernice", "Noelyn", "Stephana", "Catina", "Faydra", "Fionna", "Nola", "Courtnay", "Vera", "Meriel", "Eleonora", "Clare", "Marsha", "Marita", "Concettina", "Kristien", "Celina", "Maryl", "Codee", "Lorraine", "Lauraine", "Sephira", "Kym", "Odette", "Ranee", "Margaux", "Debra", "Corenda", "Mariejeanne", "Georgeanne", "Laural", "Fredelia", "Dulcine", "Tess", "Tina", "Adaline", "Melisandra", "Lita", "Nettie", "Lesley", "Clea", "Marysa", "Arleyne", "Meade", "Ella", "Theodora", "Morgan", "Carena", "Camille", "Janene", "Anett", "Camellia", "Guglielma", "Evvy", "Shayna", "Karilynn", "Ingeberg", "Maudie", "Colene", "Kelcy", "Blythe", "Lacy", "Cesya", "Bobbe", "Maggi", "Darline", "Almira", "Constantia", "Helaina", "Merrili", "Maxine", "Linea", "Marley", "Timmie", "Devon", "Mair", "Thomasine", "Sherry", "Gilli", "Ursa", "Marlena", "Cloris", "Vale", "Alexandra", "Angel", "Alice", "Ulrica", "Britteny", "Annie", "Juliane", "Candida", "Jennie", "Susanne", "Robenia", "Benny", "Cassy", "Denyse", "Jackquelin", "Lorelle", "Lenore", "Sheryl", "Marice", "Clarissa", "Kippy", "Cristen", "Hanni", "Marne", "Melody", "Shane", "Kalli", "Deane", "Kaila", "Faye", "Noella", "Penni", "Sophia", "Marilin", "Cori", "Clair", "Morna", "Lynn", "Rozelle", "Berta", "Bamby", "Janifer", "Doro", "Beryle", "Pammy", "Paige", "Juanita", "Ellene", "Kora", "Kerrie", "Perrine", "Dorena", "Mady", "Dorian", "Lucine", "Jill", "Octavia", "Sande", "Talyah", "Rafaelia", "Doris", "Patti", "Mora", "Marja", "Rivi", "Drucill", "Marina", "Rennie", "Annabell", "Xylia", "Zorina", "Ashil", "Becka", "Blithe", "Lenora", "Kattie", "Analise", "Jasmin", "Minetta", "Deeanne", "Sharity", "Merci", "Julissa", "Nicoli", "Nevsa", "Friederike", "Caroljean", "Catlee", "Charil", "Dara", "Kristy", "Ag", "Andriette", "Kati", "Jackqueline", "Letti", "Allys", "Carlee", "Frannie", "Philis", "Aili", "Else", "Diane", "Tobey", "Tildie", "Merrilee", "Pearle", "Christan", "Dominique", "Rosemaria", "Bunnie", "Tedi", "Elinor", "Aeriell", "Karissa", "Darya", "Tonye", "Alina", "Nalani", "Marcela", "Anabelle", "Layne", "Dorice", "Aleda", "Anette", "Arliene", "Rosemarie", "Pru", "Tiffani", "Addi", "Roda", "Shandra", "Wendeline", "Karoline", "Ciel", "Ania"
            )
            lotsOfUsers = usernames
            // if you want to make a lot more names, uncomment below
            // lotsOfUsers = (1..10).flatMap { n -> usernames.map { u -> "$u$n" } }.toList()
        }
        logInfo("It took $timeToMakeUsernames milliseconds to create ${lotsOfUsers.size} usernames")
        return lotsOfUsers
    }

}