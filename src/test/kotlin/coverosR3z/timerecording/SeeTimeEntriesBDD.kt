package coverosR3z.timerecording

import coverosR3z.*
import coverosR3z.domainobjects.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * As an employee
   I want to see the time entries I have previously entered
   So that I can confirm my time on work has been accounted correctly
 */
class SeeTimeEntriesBDD {

    @Test
    fun `happy path - should be able to get my time entries on a date`() {
        // Given I have recorded some time entries
        val (tru, entries) = recordSomeEntriesInDatabase()

        // When I request my time entries on a specific date
        val dbEntries = tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020)

        // Then I see all of them
        allEntriesArePresentOnDate(entries, dbEntries, A_RANDOM_DAY_IN_JUNE_2020)
    }

    @Test
    fun `should be able to obtain all my time entries`() {
        // Given I have recorded some time entries
        val (tru, entries) = recordSomeEntriesInDatabase()

        // When I request my time entries
        val dbEntries = tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE.id)

        // Then I see all of them
        allEntriesArePresent(entries, dbEntries.flatMap { it.value }.toSet())
    }

    @Test
    fun `there should be no entries on a given date if they have not been recorded yet`(){
        // Given no time entries were made on a day
        val (tru, _) = generateSomeEntriesPreDatabase()

        // When I ask for the time entries of that day
        val dbEntries = tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020)

        // Then I am returned nothing
        assertEquals(emptySet<TimeEntry>(), dbEntries)
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */


    private fun allEntriesArePresentOnDate(entries: List<TimeEntryPreDatabase>, dbEntries: Set<TimeEntry>, entryDate: Date) {
        val todayEntries = entries.filter { e -> e.date == entryDate }
        allEntriesArePresent(todayEntries, dbEntries)
    }

    private fun allEntriesArePresent(entries: List<TimeEntryPreDatabase>, dbEntries: Set<TimeEntry>) {
        assertEquals(entries.size, dbEntries.size)

        for (entry in entries) {
            // we should find exactly one that matches it in the ones we pulled from the database
            dbEntries.single { d -> d.toTimeEntryPreDatabase() == entry }
        }
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