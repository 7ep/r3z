package coverosR3z.persistence

import coverosR3z.*
import coverosR3z.domainobjects.*
import coverosR3z.exceptions.EmployeeNotRegisteredException
import coverosR3z.logging.logInfo
import coverosR3z.misc.getTime
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.IllegalArgumentException

class PureMemoryDatabaseTests {

    private lateinit var pmd: PureMemoryDatabase

    @Before
    fun init() {
        pmd = PureMemoryDatabase()
    }

    @Test
    fun `should be able to add a new project`() {
        pmd.addNewProject(DEFAULT_PROJECT_NAME)

        val project = pmd.getProjectById(DEFAULT_PROJECT.id)

        assertEquals(ProjectId(1), project.id)
    }

    @Test
    fun `should be able to add a new employee`() {
        pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)

        val employee = pmd.getEmployeeById(DEFAULT_EMPLOYEE.id)

        assertEquals(1, employee.id.value)
    }

    @Test
    fun `should be able to add a new time entry`() {
        pmd.addTimeEntry(TimeEntryPreDatabase(DEFAULT_EMPLOYEE, DEFAULT_PROJECT, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020))

        val timeEntries = pmd.getAllTimeEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020).first()

        assertEquals(1, timeEntries.id)
        assertEquals(DEFAULT_EMPLOYEE, timeEntries.employee)
        assertEquals(DEFAULT_PROJECT, timeEntries.project)
        assertEquals(DEFAULT_TIME, timeEntries.time)
        assertEquals(A_RANDOM_DAY_IN_JUNE_2020, timeEntries.date)
    }

    @Test
    fun `PERFORMANCE a firm should get responses from the database quite quickly`() {
        val numberOfEmployees = 30
        val numberOfProjects = 30
        val numberOfDays = 31

        val allEmployees = recordManyTimeEntries(numberOfEmployees, numberOfProjects, numberOfDays)

        val (totalTime) = getTime {
            readTimeEntriesForOneEmployee(allEmployees)
            accumulateMinutesPerEachEmployee(allEmployees)
        }

        logInfo("It took a total of $totalTime milliseconds for this code")
        assertTrue(totalTime < 100)
    }

    @Test
    fun `should be able to get the minutes on a certain date`() {
        pmd.addNewEmployee(DEFAULT_EMPLOYEE.name)
        pmd.addTimeEntry(TimeEntryPreDatabase(DEFAULT_EMPLOYEE, DEFAULT_PROJECT, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020))

        val minutes = pmd.getMinutesRecordedOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals(DEFAULT_TIME, minutes)
    }

    @Test
    fun `should not be able to get minutes recorded for an unregistered employee`() {
        assertThrows(EmployeeNotRegisteredException::class.java) {pmd.getMinutesRecordedOnDate(Employee(EmployeeId(7), EmployeeName("Harold")), A_RANDOM_DAY_IN_JUNE_2020)}
    }

    /**
     * If I ask the database for all the time entries for a particular employee on
     * a date and there aren't any, I should get back an empty list, not a null.
     */
    @Test
    fun testShouldReturnEmptyListIfNoEntries() {
        val result = pmd.getAllTimeEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020)
        assertEquals(emptySet<TimeEntry>() , result)
    }

    /**
     * If a user successfully authenticates, we should create a session entry,
     */
    @Test
    fun testShouldAddSession() {
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)
        assertEquals(DEFAULT_USER, pmd.getUserBySessionToken(DEFAULT_SESSION_TOKEN))
    }

    /**
     * If we try to add a session for a user when one already exists, throw exception
     */
    @Test
    fun testShouldAddSession_Duplicate() {
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)
        val ex = assertThrows(IllegalArgumentException::class.java) {pmd.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)}
        assertEquals("There must not already exist a session for (${DEFAULT_USER.name}) if we are to create one", ex.message)
    }

    /**
     * When a user is no longer authenticated, we enact that
     * by removing their entry from the sessions.
     */
    @Test
    fun testShouldRemoveSession() {
        pmd.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)
        assertEquals(DEFAULT_USER, pmd.getUserBySessionToken(DEFAULT_SESSION_TOKEN))
        pmd.removeSessionByToken(DEFAULT_SESSION_TOKEN)
        assertEquals(NO_USER, pmd.getUserBySessionToken(DEFAULT_SESSION_TOKEN))
    }

    /**
     * If we try to remove a session but it doesn't exist, throw an exception
     */
    @Test
    fun testShouldComplainIfTryingToRemoveNonexistentSession() {
        val ex = assertThrows(java.lang.IllegalStateException::class.java) {pmd.removeSessionByToken(DEFAULT_SESSION_TOKEN)}
        assertEquals("There must exist a session in the database for (${DEFAULT_SESSION_TOKEN}) in order to delete it", ex.message)
    }

    /**
     * I wish to make an exact copy of the PMD in completely new memory locations
     */
    @Test
    fun testShouldBePossibleToCopy_different() {
        val originalPmd = pmd.copy()
        pmd.addNewEmployee(DEFAULT_EMPLOYEE_NAME)
        assertNotEquals("after adding a new employee, the databases should differ", originalPmd, pmd)
    }

    /**
     * I wish to make an exact copy of the PMD in completely new memory locations
     */
    @Test
    fun testShouldBePossibleToCopy_similar() {
        val originalPmd = pmd.copy()
        assertEquals(originalPmd, pmd)
    }

    /**
     * Test writing the whole database and reading the whole database
     */
    @Test
    fun testShouldWriteAndReadToDisk_PERFORMANCE() {
        val numberOfEmployees = 3
        val numberOfProjects = 5
        val numberOfDays = 2
        val maxMillisecondsAllowed = 75
        val directory = File("build/db/")
        directory.deleteRecursively()
        pmd = PureMemoryDatabase.start(directory)
        directory.mkdirs()

        recordManyTimeEntries(numberOfEmployees, numberOfProjects, numberOfDays)

        val (totalTime) = getTime {
            val (timeToReadText, deserializedPmd) = getTime {PureMemoryDatabase.deserializeFromDisk(directory)}
            logInfo("it took $timeToReadText milliseconds to deserialize from disk")

            val (timeToAssert) = getTime {assertEquals(pmd, deserializedPmd)}
            logInfo("it took $timeToAssert milliseconds to assert the databases were equal")
        }

        logInfo("Total time taken for serialization / deserialzation was $totalTime milliseconds")

        assertTrue("totaltime was supposed to take $maxMillisecondsAllowed.  took $totalTime", totalTime < maxMillisecondsAllowed)
    }

    /**
     * How long does it actually take to serialize and deserialize?
     * Not very long at all.
     */
    @Test
    fun testSerializingTimeEntries_PERFORMANCE() {
        val numTimeEntries = 1000
        val timeEntries: MutableSet<TimeEntry> = prepareSomeRandomTimeEntries(numTimeEntries)

        val (timeToSerialize, serializedTimeEntries) = getTime{PureMemoryDatabase.serializeTimeEntries(timeEntries)}
        val (timeToDeserialize, _) = getTime{PureMemoryDatabase.deserializeTimeEntries(serializedTimeEntries)}

        logInfo("Time to serialize $numTimeEntries time entries was $timeToSerialize milliseconds")
        logInfo("Time to deserialize $numTimeEntries time entries was $timeToDeserialize milliseconds")

        assertTrue(timeToSerialize < 150)
        assertTrue(timeToDeserialize < 150)
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    private fun prepareSomeRandomTimeEntries(numTimeEntries: Int): MutableSet<TimeEntry> {
        val timeEntries: MutableSet<TimeEntry> = mutableSetOf()
        for (i in 1..numTimeEntries) {
            timeEntries.add(
                TimeEntry(
                    i,
                    Employee(EmployeeId(i), EmployeeName("tester$i")),
                    Project(ProjectId(i), ProjectName("someGreatProject$i")),
                    Time(100),
                    A_RANDOM_DAY_IN_JUNE_2020,
                    Details("I was lazing on a sunday afternoon")
                )
            )
        }
        return timeEntries
    }

    private fun recordManyTimeEntries(numberOfEmployees: Int, numberOfProjects: Int, numberOfDays: Int) : List<Employee> {
        val lotsOfEmployees: List<String> = generateEmployeeNames()
        persistEmployeesToDatabase(numberOfEmployees, lotsOfEmployees)
        val allEmployees: List<Employee> = readEmployeesFromDatabase()
        persistProjectsToDatabase(numberOfProjects)
        val allProjects: List<Project> = readProjectsFromDatabase()
        enterTimeEntries(numberOfDays, allEmployees, allProjects, numberOfEmployees)
        return allEmployees
    }

    private fun accumulateMinutesPerEachEmployee(allEmployees: List<Employee>) {
        val (timeToAccumulate) = getTime {
            val minutesPerEmployeeTotal =
                    allEmployees.map { e -> pmd.getAllTimeEntriesForEmployee(e).flatMap { it.value }.sumBy { te -> te.time.numberOfMinutes } }
                            .toList()
            logInfo("the time ${allEmployees[0].name.value} spent was ${minutesPerEmployeeTotal[0]}")
            logInfo("the time ${allEmployees[1].name.value} spent was ${minutesPerEmployeeTotal[1]}")
        }

        logInfo("It took $timeToAccumulate milliseconds to accumulate the minutes per employee")
    }

    private fun readTimeEntriesForOneEmployee(allEmployees: List<Employee>) {
        val (timeToGetAllTimeEntries) = getTime { pmd.getAllTimeEntriesForEmployee(allEmployees[0]) }
        logInfo("It took $timeToGetAllTimeEntries milliseconds to get all the time entries for a employee")
    }

    private fun enterTimeEntries(numberOfDays: Int, allEmployees: List<Employee>, allProjects: List<Project>, numberOfEmployees: Int) {
        val (timeToEnterAllTimeEntries) = getTime {
            for (day in 1..numberOfDays) {
                for (employee in allEmployees) {
                    pmd.addTimeEntry(TimeEntryPreDatabase(employee, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                    pmd.addTimeEntry(TimeEntryPreDatabase(employee, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                    pmd.addTimeEntry(TimeEntryPreDatabase(employee, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                    pmd.addTimeEntry(TimeEntryPreDatabase(employee, allProjects.random(), Time(2 * 60), Date(18438 + day), Details("AAAAAAAAAAAA")))
                }
            }
        }
        logInfo("It took $timeToEnterAllTimeEntries milliseconds total to enter ${numberOfDays * 4} time entries for each of $numberOfEmployees employees")
        logInfo("(That's a total of ${("%,d".format(numberOfDays * 4 * numberOfEmployees))} time entries)")
    }

    private fun readProjectsFromDatabase(): List<Project> {
        val (timeToReadAllProjects, allProjects) = getTime { pmd.getAllProjects()}
        logInfo("It took $timeToReadAllProjects milliseconds to read all the projects")
        return allProjects
    }

    private fun persistProjectsToDatabase(numberOfProjects: Int) {
        val (timeToCreateProjects) =
                getTime { (0..numberOfProjects).forEach { i -> pmd.addNewProject(ProjectName("project$i")) } }
        logInfo("It took $timeToCreateProjects milliseconds to create $numberOfProjects projects")
    }

    private fun readEmployeesFromDatabase(): List<Employee> {
        val (timeToReadAllEmployees, allEmployees) = getTime {
            pmd.getAllEmployees()
        }
        logInfo("It took $timeToReadAllEmployees milliseconds to read all the employees")
        return allEmployees
    }

    private fun persistEmployeesToDatabase(numberOfEmployees: Int, lotsOfEmployees: List<String>) {
        val (timeToEnterEmployees) = getTime {
            for (i in 0..numberOfEmployees) {
                pmd.addNewEmployee(EmployeeName(lotsOfEmployees[i]))
            }
        }
        logInfo("It took $timeToEnterEmployees milliseconds to enter $numberOfEmployees employees")
    }

    private fun generateEmployeeNames(): List<String> {
        val (timeToMakeEmployeenames, lotsOfEmployees) = getTime {
            listOf(
                    "Arlen", "Hedwig", "Allix", "Tandi", "Silvia", "Catherine", "Mavis", "Hally", "Renate", "Anastasia", "Christy", "Nora", "Molly", "Nelli", "Daphna", "Chloette", "TEirtza", "Nannie", "Melinda", "Tyne", "Belva", "Pam", "Rebekkah", "Elayne", "Dianne", "Christina", "Jeanne", "Norry", "Reina", "Erminia", "Eadie", "Valina", "Gayle", "Wylma", "Annette", "Annmaria", "Fayina", "Dita", "Sibella", "Alis", "Georgena", "Luciana", "Sidonnie", "Dina", "Ferdinande", "Coletta", "Esma", "Saidee", "Hannah", "Colette", "Anitra", "Grissel", "Caritta", "Ann", "Rhodia", "Meta", "Bride", "Dalenna", "Rozina", "Ottilie", "Eliza", "Gerda", "Anthia", "Kathryn", "Lilian", "Jeannie", "Nichole", "Marylinda", "Angelica", "Margie", "Ruthie", "Augustina", "Netta", "Fleur", "Mahala", "Cosette", "Zsa Zsa", "Karry", "Tabitha", "Andriana", "Fey", "Hedy", "Saudra", "Geneva", "Lacey", "Fawnia", "Ertha", "Bernie", "Natty", "Joyan", "Teddie", "Hephzibah", "Vonni", "Ambur", "Lyndsie", "Anna", "Minnaminnie", "Andy", "Brina", "Pamella", "Trista", "Antonetta", "Kerrin", "Crysta", "Kira", "Gypsy", "Candy", "Ree", "Sharai", "Mariana", "Eleni", "Yetty", "Maisie", "Deborah", "Doretta", "Juliette", "Esta", "Amandi", "Anallise", "Indira", "Aura", "Melodee", "Desiri", "Jacquenetta", "Joell", "Delcine", "Justine", "Theresita", "Belia", "Mallory", "Antonie", "Jobi", "Katalin", "Kelli", "Ester", "Katey", "Gianna", "Berry", "Sidonia", "Roseanne", "Cherida", "Beatriz", "Eartha", "Robina", "Florri", "Vitoria", "Debera", "Jeanette", "Almire", "Saree", "Liana", "Ruth", "Renell", "Katinka", "Anya", "Gwyn", "Kaycee", "Rori", "Rianon", "Joann", "Zorana", "Hermia", "Gwenni", "Poppy", "Dedie", "Cloe", "Kirsti", "Krysta", "Clarinda", "Enid", "Katina", "Ralina", "Meryl", "Andie", "Orella", "Alexia", "Clarey", "Iris", "Chris", "Devin", "Kally", "Vernice", "Noelyn", "Stephana", "Catina", "Faydra", "Fionna", "Nola", "Courtnay", "Vera", "Meriel", "Eleonora", "Clare", "Marsha", "Marita", "Concettina", "Kristien", "Celina", "Maryl", "Codee", "Lorraine", "Lauraine", "Sephira", "Kym", "Odette", "Ranee", "Margaux", "Debra", "Corenda", "Mariejeanne", "Georgeanne", "Laural", "Fredelia", "Dulcine", "Tess", "Tina", "Adaline", "Melisandra", "Lita", "Nettie", "Lesley", "Clea", "Marysa", "Arleyne", "Meade", "Ella", "Theodora", "Morgan", "Carena", "Camille", "Janene", "Anett", "Camellia", "Guglielma", "Evvy", "Shayna", "Karilynn", "Ingeberg", "Maudie", "Colene", "Kelcy", "Blythe", "Lacy", "Cesya", "Bobbe", "Maggi", "Darline", "Almira", "Constantia", "Helaina", "Merrili", "Maxine", "Linea", "Marley", "Timmie", "Devon", "Mair", "Thomasine", "Sherry", "Gilli", "Ursa", "Marlena", "Cloris", "Vale", "Alexandra", "Angel", "Alice", "Ulrica", "Britteny", "Annie", "Juliane", "Candida", "Jennie", "Susanne", "Robenia", "Benny", "Cassy", "Denyse", "Jackquelin", "Lorelle", "Lenore", "Sheryl", "Marice", "Clarissa", "Kippy", "Cristen", "Hanni", "Marne", "Melody", "Shane", "Kalli", "Deane", "Kaila", "Faye", "Noella", "Penni", "Sophia", "Marilin", "Cori", "Clair", "Morna", "Lynn", "Rozelle", "Berta", "Bamby", "Janifer", "Doro", "Beryle", "Pammy", "Paige", "Juanita", "Ellene", "Kora", "Kerrie", "Perrine", "Dorena", "Mady", "Dorian", "Lucine", "Jill", "Octavia", "Sande", "Talyah", "Rafaelia", "Doris", "Patti", "Mora", "Marja", "Rivi", "Drucill", "Marina", "Rennie", "Annabell", "Xylia", "Zorina", "Ashil", "Becka", "Blithe", "Lenora", "Kattie", "Analise", "Jasmin", "Minetta", "Deeanne", "Sharity", "Merci", "Julissa", "Nicoli", "Nevsa", "Friederike", "Caroljean", "Catlee", "Charil", "Dara", "Kristy", "Ag", "Andriette", "Kati", "Jackqueline", "Letti", "Allys", "Carlee", "Frannie", "Philis", "Aili", "Else", "Diane", "Tobey", "Tildie", "Merrilee", "Pearle", "Christan", "Dominique", "Rosemaria", "Bunnie", "Tedi", "Elinor", "Aeriell", "Karissa", "Darya", "Tonye", "Alina", "Nalani", "Marcela", "Anabelle", "Layne", "Dorice", "Aleda", "Anette", "Arliene", "Rosemarie", "Pru", "Tiffani", "Addi", "Roda", "Shandra", "Wendeline", "Karoline", "Ciel", "Ania"
            )
            // if you want to make a lot more names, uncomment below
            // lotsOfEmployees = (1..10).flatMap { n -> employeenames.map { u -> "$u$n" } }.toList()
        }
        logInfo("It took $timeToMakeEmployeenames milliseconds to create ${lotsOfEmployees.size} employeenames")
        return lotsOfEmployees
    }

}