package coverosR3z.authentication.utility

import coverosR3z.authentication.FakeAuthPersistence
import coverosR3z.authentication.types.*
import coverosR3z.misc.*
import coverosR3z.timerecording.FakeTimeEntryPersistence
import coverosR3z.timerecording.types.*
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Test

class RoleVerificationTests {

    /*
    Regular:
    - make entries
    - edit entries
    - delete entries
    - submit/unsubmit periods
    - view all projects
    - view all employees
    - view all time entries

    Approver
    - everything a regular user can do
    - can unsubmit periods for others
    - can approve submitted periods

    Administrator
    - everything a regular user can do
    - can unsubmit periods for others
    - create employees
    - create projects
    - delete projects
    - hey what happens if you delete a project their are entries for?

    System
    - change user
    - delete entries
    - delete users
    - delete projects
    - register users
    - login users

    */

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
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_USER)
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

//    - edit entries
//    - delete entries
//    - submit/unsubmit periods

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

    @Test
    fun systemRoleCanCreateSession() {
        val (au, rc) = makeAuthUtils(CurrentUser(SYSTEM_USER))
        au.createNewSession(DEFAULT_USER)
        assertTrue(rc.roleCanDoAction)
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