package coverosR3z.timerecording

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.FakeRolesChecker
import coverosR3z.misc.*
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Test

class TimeRecordingUtilitiesRoleTests {

    /*
                        _                    _       _          _
      _ _ ___ __ _ _  _| |__ _ _ _   _ _ ___| |___  | |_ ___ __| |_ ___
     | '_/ -_) _` | || | / _` | '_| | '_/ _ \ / -_) |  _/ -_|_-<  _(_-<
     |_| \___\__, |\_,_|_\__,_|_|   |_| \___/_\___|  \__\___/__/\__/__/
             |___/
    alt-text: regular role tests
    font: small
    */
    @Test
    fun testRegularRole() {
        val (tru, frc) = makeTRU(DEFAULT_REGULAR_USER)

        tru.changeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)

        tru.changeUser(CurrentUser(DEFAULT_REGULAR_USER))
        assertFalse(frc.roleCanDoAction)

        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        assertFalse(frc.roleCanDoAction)

        tru.createProject(DEFAULT_PROJECT_NAME)
        assertFalse(frc.roleCanDoAction)

        tru.createTimeEntry(DEFAULT_TIME_ENTRY.toTimeEntryPreDatabase())
        assertTrue(frc.roleCanDoAction)

        tru.deleteTimeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)

        tru.findEmployeeById(DEFAULT_EMPLOYEE.id)
        assertTrue(frc.roleCanDoAction)

        tru.findProjectById(DEFAULT_PROJECT.id)
        assertTrue(frc.roleCanDoAction)

        tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE)
        assertTrue(frc.roleCanDoAction)

        tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertTrue(frc.roleCanDoAction)

        tru.getSubmittedTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.getTimeEntriesForTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.isInASubmittedPeriod(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertTrue(frc.roleCanDoAction)

        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)

        tru.listAllProjects()
        assertTrue(frc.roleCanDoAction)

        tru.submitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.unsubmitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)
    }

    /*
              _       _                _       _          _
      __ _ __| |_ __ (_)_ _    _ _ ___| |___  | |_ ___ __| |_ ___
     / _` / _` | '  \| | ' \  | '_/ _ \ / -_) |  _/ -_|_-<  _(_-<
     \__,_\__,_|_|_|_|_|_||_| |_| \___/_\___|  \__\___/__/\__/__/
    alt-text: admin role tests
    font: small
     */
    @Test
    fun testAdminRole() {
        val (tru, frc) = makeTRU(DEFAULT_ADMIN_USER)

        tru.changeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)

        tru.changeUser(CurrentUser(DEFAULT_REGULAR_USER))
        assertFalse(frc.roleCanDoAction)

        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        assertTrue(frc.roleCanDoAction)

        tru.createProject(DEFAULT_PROJECT_NAME)
        assertTrue(frc.roleCanDoAction)

        tru.createTimeEntry(DEFAULT_TIME_ENTRY.toTimeEntryPreDatabase())
        assertTrue(frc.roleCanDoAction)

        tru.deleteTimeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)

        tru.findEmployeeById(DEFAULT_EMPLOYEE.id)
        assertTrue(frc.roleCanDoAction)

        tru.findProjectById(DEFAULT_PROJECT.id)
        assertTrue(frc.roleCanDoAction)

        tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE)
        assertTrue(frc.roleCanDoAction)

        tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertTrue(frc.roleCanDoAction)

        tru.getSubmittedTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.getTimeEntriesForTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.isInASubmittedPeriod(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertTrue(frc.roleCanDoAction)

        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)

        tru.listAllProjects()
        assertTrue(frc.roleCanDoAction)

        tru.submitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.unsubmitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)
    }

    /*
             _                       _       _          _
  ____  _ __| |_ ___ _ __    _ _ ___| |___  | |_ ___ __| |_ ___
 (_-< || (_-<  _/ -_) '  \  | '_/ _ \ / -_) |  _/ -_|_-<  _(_-<
 /__/\_, /__/\__\___|_|_|_| |_| \___/_\___|  \__\___/__/\__/__/
     |__/
     */
    @Test
    fun testSystemRole() {
        val (tru, frc) = makeTRU(SYSTEM_USER)

        tru.changeEntry(DEFAULT_TIME_ENTRY)
        assertFalse(frc.roleCanDoAction)

        tru.changeUser(CurrentUser(DEFAULT_REGULAR_USER))
        assertTrue(frc.roleCanDoAction)

        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        assertTrue(frc.roleCanDoAction)

        tru.createProject(DEFAULT_PROJECT_NAME)
        assertTrue(frc.roleCanDoAction)

        tru.createTimeEntry(DEFAULT_TIME_ENTRY.toTimeEntryPreDatabase())
        assertFalse(frc.roleCanDoAction)

        tru.deleteTimeEntry(DEFAULT_TIME_ENTRY)
        assertFalse(frc.roleCanDoAction)

        tru.findEmployeeById(DEFAULT_EMPLOYEE.id)
        assertFalse(frc.roleCanDoAction)

        tru.findProjectById(DEFAULT_PROJECT.id)
        assertFalse(frc.roleCanDoAction)

        tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE)
        assertFalse(frc.roleCanDoAction)

        tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertFalse(frc.roleCanDoAction)

        tru.getSubmittedTimePeriod(DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)

        tru.getTimeEntriesForTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)

        tru.isInASubmittedPeriod(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertFalse(frc.roleCanDoAction)

        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)

        tru.listAllProjects()
        assertFalse(frc.roleCanDoAction)

        tru.submitTimePeriod(DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)

        tru.unsubmitTimePeriod(DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)
    }

    /*
                                               _       _          _
  __ _ _ __ _ __ _ _ _____ _____ _ _   _ _ ___| |___  | |_ ___ __| |_ ___
 / _` | '_ \ '_ \ '_/ _ \ V / -_) '_| | '_/ _ \ / -_) |  _/ -_|_-<  _(_-<
 \__,_| .__/ .__/_| \___/\_/\___|_|   |_| \___/_\___|  \__\___/__/\__/__/
      |_|  |_|
     */
    @Test
    fun testApproverRole() {
        val (tru, frc) = makeTRU(DEFAULT_REGULAR_USER)

        tru.changeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)

        tru.changeUser(CurrentUser(DEFAULT_REGULAR_USER))
        assertFalse(frc.roleCanDoAction)

        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        assertFalse(frc.roleCanDoAction)

        tru.createProject(DEFAULT_PROJECT_NAME)
        assertFalse(frc.roleCanDoAction)

        tru.createTimeEntry(DEFAULT_TIME_ENTRY.toTimeEntryPreDatabase())
        assertTrue(frc.roleCanDoAction)

        tru.deleteTimeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)

        tru.findEmployeeById(DEFAULT_EMPLOYEE.id)
        assertTrue(frc.roleCanDoAction)

        tru.findProjectById(DEFAULT_PROJECT.id)
        assertTrue(frc.roleCanDoAction)

        tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE)
        assertTrue(frc.roleCanDoAction)

        tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertTrue(frc.roleCanDoAction)

        tru.getSubmittedTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.getTimeEntriesForTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.isInASubmittedPeriod(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertTrue(frc.roleCanDoAction)

        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)

        tru.listAllProjects()
        assertTrue(frc.roleCanDoAction)

        tru.submitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.unsubmitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)
    }

    /*
                              _       _          _
  _ _  ___ _ _  ___   _ _ ___| |___  | |_ ___ __| |_ ___
 | ' \/ _ \ ' \/ -_) | '_/ _ \ / -_) |  _/ -_|_-<  _(_-<
 |_||_\___/_||_\___| |_| \___/_\___|  \__\___/__/\__/__/

     */
    @Test
    fun testNoneRole() {
        val (tru, frc) = makeTRU(NO_USER)

        tru.changeEntry(DEFAULT_TIME_ENTRY)
        assertFalse(frc.roleCanDoAction)

        tru.changeUser(CurrentUser(DEFAULT_REGULAR_USER))
        assertFalse(frc.roleCanDoAction)

        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        assertFalse(frc.roleCanDoAction)

        tru.createProject(DEFAULT_PROJECT_NAME)
        assertFalse(frc.roleCanDoAction)

        tru.createTimeEntry(DEFAULT_TIME_ENTRY.toTimeEntryPreDatabase())
        assertFalse(frc.roleCanDoAction)

        tru.deleteTimeEntry(DEFAULT_TIME_ENTRY)
        assertFalse(frc.roleCanDoAction)

        tru.findEmployeeById(DEFAULT_EMPLOYEE.id)
        assertFalse(frc.roleCanDoAction)

        tru.findProjectById(DEFAULT_PROJECT.id)
        assertFalse(frc.roleCanDoAction)

        tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE)
        assertFalse(frc.roleCanDoAction)

        tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertFalse(frc.roleCanDoAction)

        tru.getSubmittedTimePeriod(DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)

        tru.getTimeEntriesForTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)

        tru.isInASubmittedPeriod(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertFalse(frc.roleCanDoAction)

        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)

        tru.listAllProjects()
        assertFalse(frc.roleCanDoAction)

        tru.submitTimePeriod(DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)

        tru.unsubmitTimePeriod(DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    private fun makeTRU(user: User = DEFAULT_ADMIN_USER): Pair<ITimeRecordingUtilities, FakeRolesChecker>{
        val frc = FakeRolesChecker(CurrentUser(user))
        val tru = TimeRecordingUtilities(FakeTimeEntryPersistence(), CurrentUser(user), testLogger, frc)
        return Pair(tru, frc)
    }
}