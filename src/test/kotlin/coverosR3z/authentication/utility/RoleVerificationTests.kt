package coverosR3z.authentication.utility

import coverosR3z.authentication.FakeAuthPersistence
import coverosR3z.authentication.types.*
import coverosR3z.misc.*
import coverosR3z.timerecording.FakeTimeEntryPersistence
import coverosR3z.timerecording.types.*
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Test

/**
 * Approver
 * - everything a regular user can do
 * - can unsubmit periods for others
 * - can approve submitted periods
 *
 * System
 * - change user
 * - delete entries
 * - delete users
 * - delete projects
 * - register users
 * - login users
 *
 * Administrator
 * - everything a regular user can do
 * - hey what happens if you delete a project there are entries for?
 * - can admins enter time for other people? (No atm)
 */
class RoleVerificationTests {

    /*
      _ _ ___ __ _ _  _| |__ _ _ _   _ _ ___| |___  | |_ ___ __| |_ ___
     | '_/ -_) _` | || | / _` | '_| | '_/ _ \ / -_) |  _/ -_|_-<  _(_-<
     |_| \___\__, |\_,_|_\__,_|_|   |_| \___/_\___|  \__\___/__/\__/__/
             |___/
    alt-text: regular role tests
    font: small
    */


    @Test
    fun regularRoleCannotCreateSession() {
        val (authUtils, rc) = makeAuthUtils(CurrentUser(DEFAULT_USER))
        authUtils.createNewSession(DEFAULT_USER)
        assertFalse(rc.roleCanDoAction)
    }

    @Test
    fun regularRoleCannotCreateProject() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_USER)
        tru.createProject(ProjectName("flim flam"))
        assertFalse(frc.roleCanDoAction)
    }

    @Test
    fun oneRegularUserCannotEnterTimeForAnother() {
        // be a user
        val (tru, _) = makeTRUWithABunchOfFakes(DEFAULT_USER)
        val otherEmployee = Employee(EmployeeId(2), DEFAULT_EMPLOYEE_NAME)
        val entry = createTimeEntryPreDatabase(employee=otherEmployee)
        val result = tru.createTimeEntry(entry)
        assertEquals(RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH, null), result)
    }

    @Test
    fun regularUserCannotCreateEmployee() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_USER)
        tru.createEmployee(EmployeeName("Doesn't matter, shouldn't work"))
        assertFalse(frc.roleCanDoAction)
    }

    //     - view all time entries
    @Test
    fun regularRoleCanViewAllTimeEntries() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_USER)
        tru.getAllEntriesForEmployee(DEFAULT_USER.employeeId)
        assertTrue(frc.roleCanDoAction)
    }

    // - view all employees
    @Test
    fun regularRoleCanViewAllEmployees() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_USER)
        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)
    }

    //    - view all projects
    @Test
    fun regularRoleCanViewAllProjects() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_USER)
        tru.listAllProjects()
        assertTrue(frc.roleCanDoAction)
    }

    //    - make entries
    @Test
    fun regularRoleCanMakeAnEntry() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_USER)
        val entry = createTimeEntryPreDatabase(employee=DEFAULT_EMPLOYEE)
        tru.createTimeEntry(entry)
        assertTrue(frc.roleCanDoAction)
    }

    //   - edit entries
    @Test
    fun regularRoleCanEditAnEntry() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_USER)
        val entry = createTimeEntryPreDatabase(employee=DEFAULT_EMPLOYEE)
        val se = tru.createTimeEntry(entry).newTimeEntry
        val entry2 = se?.let { TimeEntry(se.id, se.employee, se.project, se.time, it.date, se.details ) }
        tru.changeEntry(entry2!!)
        assertTrue(frc.roleCanDoAction)
    }

    //    - submit/unsubmit periods
    @Test
    fun regularRoleCanSubmitAndUnsubmitAPeriod() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_USER)
        val period = TimePeriod.getTimePeriodForDate(A_RANDOM_DAY_IN_JUNE_2020)
        tru.submitTimePeriod(period)
        assertTrue(frc.roleCanDoAction)
        tru.unsubmitTimePeriod(period)
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
    fun adminRoleCanCreateProject() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_ADMIN_USER)
        tru.createProject(ProjectName("flim flam"))
        print(frc.roleCanDoAction)
        assertTrue(frc.roleCanDoAction)
    }

    @Test
    fun adminRoleCanCreateEmployee() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_ADMIN_USER)
        tru.createEmployee(EmployeeName("Doesn't matter, shouldn't work"))
        assertTrue(frc.roleCanDoAction)
    }

    //I'm gonna take you on a journey byron
    @Test
    fun adminRoleCanEnterWhateverTimeForWhoeverTheyPlease() { // So maybe they are not supposed to be able to
        // be an admin
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_ADMIN_USER)
        val otherEmployee = Employee(EmployeeId(2), DEFAULT_EMPLOYEE_NAME)
        val entry = createTimeEntryPreDatabase(employee=otherEmployee)

        tru.createTimeEntry(entry)
        assertTrue(frc.roleCanDoAction)
    }

    //need to further implement unsubmit powers to test if we can unsubmit other people's junk
    @Test
    fun adminRoleCanUnsubmitAPeriod() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_ADMIN_USER)
        tru.unsubmitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)
    }

    // - view all employees
    @Test
    fun adminRoleCanViewAllEmployees() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_ADMIN_USER)
        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)
    }

    //    - view all projects
    @Test
    fun adminRoleCanViewAllProjects() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_ADMIN_USER)
        tru.listAllProjects()
        assertTrue(frc.roleCanDoAction)
    }

    @Test
    fun adminRoleCanViewAllTimeEntries() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_ADMIN_USER)
        tru.getAllEntriesForEmployee(DEFAULT_ADMIN_USER.employeeId)
        assertTrue(frc.roleCanDoAction)
    }

    //    - make entries
    @Test
    fun adminRoleCanMakeAnEntry() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_ADMIN_USER)
        val entry = createTimeEntryPreDatabase(employee=DEFAULT_EMPLOYEE)
        tru.createTimeEntry(entry)
        assertTrue(frc.roleCanDoAction)
    }

    //   - edit entries
    @Test
    fun adminRoleCanEditAnEntry() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_ADMIN_USER)
        tru.changeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)
    }

    //TODO SYSTEM ROLE TESTS - Matt make this pretty!

    @Test
    fun systemRoleCanCreateSession() {
        val (au, rc) = makeAuthUtils(CurrentUser(SYSTEM_USER))
        au.createNewSession(DEFAULT_USER)
        assertTrue(rc.roleCanDoAction)
    }

    //TODO APPROVER ROLE TESTS - MAAAAAATTTTTTTTT

    @Test
    fun approverRoleCannotCreateSession() {
        val (authUtils, rc) = makeAuthUtils(CurrentUser(DEFAULT_APPROVER))
        authUtils.createNewSession(DEFAULT_APPROVER)
        assertFalse(rc.roleCanDoAction)
    }

    @Test
    fun approverRoleCannotCreateProject() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_APPROVER)
        tru.createProject(ProjectName("flim flam floozy"))
        assertFalse(frc.roleCanDoAction)
    }

    @Test
    fun approverUserCannotEnterTimeForAnother() {
        // be a user
        val (tru, _) = makeTRUWithABunchOfFakes(DEFAULT_APPROVER)
        val otherEmployee = Employee(EmployeeId(2), DEFAULT_EMPLOYEE_NAME)
        val entry = createTimeEntryPreDatabase(employee=otherEmployee)
        val result = tru.createTimeEntry(entry)
        assertEquals(RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH, null), result)
    }

    @Test
    fun approverUserCannotCreateEmployee() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_APPROVER)
        tru.createEmployee(EmployeeName("Spartacus"))
        assertFalse(frc.roleCanDoAction)
    }

    //     - view all time entries
    @Test
    fun approverRoleCanViewAllTimeEntries() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_APPROVER)
        tru.getAllEntriesForEmployee(DEFAULT_APPROVER.employeeId)
        assertTrue(frc.roleCanDoAction)
    }

    // - view all employees
    @Test
    fun approverRoleCanViewAllEmployees() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_APPROVER)
        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)
    }

    //    - view all projects
    @Test
    fun approverRoleCanViewAllProjects() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_APPROVER)
        tru.listAllProjects()
        assertTrue(frc.roleCanDoAction)
    }

    //    - make entries
    @Test
    fun approverRoleCanMakeAnEntry() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_APPROVER)
        val entry = createTimeEntryPreDatabase(employee=DEFAULT_EMPLOYEE)
        tru.createTimeEntry(entry)
        assertTrue(frc.roleCanDoAction)
    }

    //   - edit entries
    @Test
    fun approverRoleCanEditAnEntry() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_APPROVER)
        tru.changeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)
    }

    //    - submit/unsubmit periods
    @Test
    fun approverRoleCanSubmitAndUnsubmitAPeriod() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_APPROVER)
        val period = TimePeriod.getTimePeriodForDate(A_RANDOM_DAY_IN_JUNE_2020)
        tru.submitTimePeriod(period)
        assertTrue(frc.roleCanDoAction)
        tru.unsubmitTimePeriod(period)
        assertTrue(frc.roleCanDoAction)
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    private fun makeAuthUtils(cu: CurrentUser): Pair<IAuthenticationUtilities, FakeRolesChecker> {
        val rc = FakeRolesChecker(cu)
        val ap = FakeAuthPersistence()
        val authUtils = AuthenticationUtilities(ap, testLogger, cu, rc)
        return Pair(authUtils, rc)
    }

    fun makeTRUWithABunchOfFakes(user: User = DEFAULT_ADMIN_USER): Pair<TimeRecordingUtilities, FakeRolesChecker>{
        val frc = FakeRolesChecker(CurrentUser(user))
        val tru = TimeRecordingUtilities(FakeTimeEntryPersistence(), CurrentUser(user), testLogger, frc)
        return Pair(tru, frc)
    }
}