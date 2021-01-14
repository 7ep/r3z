package coverosR3z.timerecording

import coverosR3z.bdd.BDD
import coverosR3z.bdd.RecordTimeUserStory
import coverosR3z.bdd.ViewTimeUserStory
import coverosR3z.misc.*
import coverosR3z.misc.types.Date
import coverosR3z.timerecording.types.*
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class SeeTimeEntriesBDD {

    @BDD
    @Test
    fun `happy path - should be able to get my time entries on a date`() {
        val s = ViewTimeUserStory.addScenario(
            "happy path - should be able to get my time entries on a date",

            listOf(
                "Given I have recorded some time entries",
                "When I request my time entries on a specific date",
                "Then I see all of them"
            )
        )

        val (tru, entries) = recordSomeEntriesInDatabase()
        s.markDone("Given I have recorded some time entries")

        val dbEntries = tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE.id, A_RANDOM_DAY_IN_JUNE_2020)
        s.markDone("When I request my time entries on a specific date")

        assertTrue(allEntriesArePresentOnDate(entries, dbEntries, A_RANDOM_DAY_IN_JUNE_2020))
        s.markDone("Then I see all of them")
    }

    @BDD
    @Test
    fun `should be able to obtain all my time entries`() {
        val s = ViewTimeUserStory.addScenario(
            "should be able to obtain all my time entries",

            listOf(
                "Given I have recorded some time entries",
                "When I request my time entries",
                "Then I see all of them"
            )
        )

        val (tru, entries) = recordSomeEntriesInDatabase()
        s.markDone("Given I have recorded some time entries")

        val dbEntries = tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE.id)
        s.markDone("When I request my time entries")

        assertTrue(allEntriesArePresent(entries, dbEntries))
        s.markDone("Then I see all of them")
    }

    @BDD
    @Test
    fun `there should be no entries on a given date if they have not been recorded yet`(){
        val s = ViewTimeUserStory.addScenario(
            "there should be no entries on a given date if they have not been recorded yet",

            listOf(
                "Given no time entries were made on a day",
                "When I ask for the time entries of that day",
                "Then I am returned nothing"
            )
        )

        val (tru, _) = generateSomeEntriesPreDatabase()
        s.markDone("Given no time entries were made on a day")

        val dbEntries = tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE.id, A_RANDOM_DAY_IN_JUNE_2020)
        s.markDone("When I ask for the time entries of that day")

        assertEquals(emptySet<TimeEntry>(), dbEntries)
        s.markDone("Then I am returned nothing")
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    private fun allEntriesArePresentOnDate(entries: List<TimeEntryPreDatabase>, dbEntries: Set<TimeEntry>, entryDate: Date) : Boolean {
        val todayEntries = entries.filter { e -> e.date == entryDate }
        return allEntriesArePresent(todayEntries, dbEntries)
    }

    private fun allEntriesArePresent(entries: List<TimeEntryPreDatabase>, dbEntries: Set<TimeEntry>) : Boolean {
        assertEquals(entries.size, dbEntries.size)

        for (entry in entries) {
            // we should find exactly one that matches it in the ones we pulled from the database
            assertEquals(1, dbEntries.count { d -> d.toTimeEntryPreDatabase() == entry })
        }
        return true
    }

    private fun generateSomeEntriesPreDatabase() : Pair<TimeRecordingUtilities, List<TimeEntryPreDatabase>>{
        val tru = createTimeRecordingUtility()
        val project1: Project = tru.createProject(ProjectName("project 1"))
        val project2: Project = tru.createProject(ProjectName("project 2"))
        val project3: Project = tru.createProject(ProjectName("project 3"))
        val newEmployee : Employee = tru.createEmployee(DEFAULT_EMPLOYEE_NAME)

        val entries : List<TimeEntryPreDatabase> = listOf(
            createTimeEntryPreDatabase(
                employee = newEmployee,
                project = project1,
                time = Time(30),
                details = Details("abc")
            ),
            createTimeEntryPreDatabase(
                employee = newEmployee,
                project = project2,
                time = Time(120),
                details = Details("def")
            ),
            // create one for the day after
            createTimeEntryPreDatabase(
                employee = newEmployee,
                project = project3,
                time = Time(120),
                details = Details("ghi"),
                date = A_RANDOM_DAY_IN_JUNE_2020_PLUS_ONE
            )
        )

        return Pair(tru, entries)
    }

    private fun recordSomeEntriesInDatabase(): Pair<TimeRecordingUtilities, List<TimeEntryPreDatabase>> {
        val (tru, entries) = generateSomeEntriesPreDatabase()

        for (entry in entries) {
            tru.recordTime(entry)
        }

        return Pair(tru, entries)
    }

}