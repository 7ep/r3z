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

    @Test fun `happy path - should be able to get my time entries on a date`() {
        val (tru, entries) = `given I have created some time entries`()

        // when I request my time entries
        val dbEntries = tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020)

        `then I see all of them on that date`(entries, dbEntries)
    }

    @Test fun `should be able to obtain all my time entries`() {
        val (tru, entries) = `given I have created some time entries`()

        // when I request my time entries
        val dbEntries = tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE)

        `then I see all of them`(entries, dbEntries)
    }

    private fun `then I see all of them on that date`(entries: List<TimeEntryPreDatabase>, dbEntries: List<TimeEntry>) {
        // then I see all of them
        val todayEntries = entries.filter { e -> e.date == A_RANDOM_DAY_IN_JUNE_2020 }
        assertEquals(todayEntries.size, dbEntries.size)

        // for each entry we added...
        for (entry in todayEntries) {
            // we should find exactly one that matches it in the ones we pulled from the database
            dbEntries.single { d -> d.toTimeEntryPreDatabase() == entry }
        }
    }

    private fun `then I see all of them`(entries: List<TimeEntryPreDatabase>, dbEntries: List<TimeEntry>) {
        // then I see all of them
        assertEquals(entries.size, dbEntries.size)

        // for each entry we added...
        for (entry in entries) {
            // we should find exactly one that matches it in the ones we pulled from the database
            dbEntries.single { d -> d.toTimeEntryPreDatabase() == entry }
        }
    }

    private fun `given I have created some time entries`(): Pair<TimeRecordingUtilities, List<TimeEntryPreDatabase>> {
        val tru = createTimeRecordingUtility()
        val project1: Project = tru.createProject(ProjectName("project 1"))
        val project2: Project = tru.createProject(ProjectName("project 2"))
        val project3: Project = tru.createProject(ProjectName("project 3"))
        val newEmployee : Employee = tru.createEmployee(DEFAULT_EMPLOYEENAME)

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

        for (entry in entries) {
            tru.recordTime(entry)
        }

        return Pair(tru, entries)
    }

}