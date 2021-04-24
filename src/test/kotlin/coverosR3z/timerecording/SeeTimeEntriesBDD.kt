package coverosR3z.timerecording

import coverosR3z.bddframework.BDD
import coverosR3z.system.misc.*
import coverosR3z.system.misc.types.Date
import coverosR3z.timerecording.types.*
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeeTimeEntriesBDD {

    @BDD
    @Test
    fun `happy path - should be able to get my time entries on a date`() {
        val s = ViewTimeUserStory.getScenario("happy path - should be able to get my time entries on a date")

        val (tru, entries) = recordSomeEntriesInDatabase()
        s.markDone("Given I have recorded some time entries")

        val dbEntries = tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020)
        s.markDone("When I request my time entries on a specific date")

        assertTrue(allEntriesArePresentOnDate(entries, dbEntries, A_RANDOM_DAY_IN_JUNE_2020))
        s.markDone("Then I see all of them")
    }

    @BDD
    @Test
    fun `should be able to obtain all my time entries`() {
        val s = ViewTimeUserStory.getScenario("should be able to obtain all my time entries")

        val (tru, entries) = recordSomeEntriesInDatabase()
        s.markDone("Given I have recorded some time entries")

        val dbEntries = tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE)
        s.markDone("When I request my time entries")

        assertTrue(allEntriesArePresent(entries, dbEntries))
        s.markDone("Then I see all of them")
    }

    @BDD
    @Test
    fun `there should be no entries on a given date if they have not been recorded yet`(){
        val s = ViewTimeUserStory.getScenario("there should be no entries on a given date if they have not been recorded yet")

        val (tru, _) = generateSomeEntriesPreDatabase()
        s.markDone("Given no time entries were made on a day")

        val dbEntries = tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020)
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
            tru.createTimeEntry(entry)
        }

        return Pair(tru, entries)
    }

}