package coverosR3z.authentication.utility

import coverosR3z.authentication.FakeAuthPersistence
import coverosR3z.authentication.exceptions.UnpermittedOperationException
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.User
import coverosR3z.misc.*
import coverosR3z.timerecording.FakeTimeEntryPersistence
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.types.ProjectName
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class RoleVerificationTests {

    private lateinit var authUtils : AuthenticationUtilities
    private lateinit var ap : FakeAuthPersistence

    @Before
    fun init() {
        ap = FakeAuthPersistence()
    }

    /*
    Thinking
    Roles:

    Employee
    can...
    * make entries for self
    * edit entries for self
    * delete entries for self (jk)
    * submit periods for self
    * unsubmit periods for self

    can't...
    * make/edit/delete entries for other users
    * make projects
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
        val rc = makeAuthUtils(CurrentUser(DEFAULT_USER))
        authUtils.createNewSession(DEFAULT_USER)
        assertFalse(rc.didAuthorize)
    }

    @Test
    fun regularRoleCannotCreateProject() {
        val (tru, frc) = makeTRUWithABunchOfFakes(DEFAULT_USER)
        assertThrows(UnpermittedOperationException::class.java) {tru.createProject(ProjectName("flim flam"))}
    }

    @Test
    fun oneRegularUserCannotEnterTimeForAnother() {

    }


    @Test
    fun systemRoleCanCreateSession() {
        val rc = makeAuthUtils(CurrentUser(SYSTEM_USER))
        authUtils.createNewSession(DEFAULT_USER)
        assertFalse(rc.didAuthorize)
    }


    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    private fun makeAuthUtils(cu: CurrentUser): FakeRolesChecker {
        val rc = FakeRolesChecker(cu)
        authUtils = AuthenticationUtilities(ap, testLogger, cu, rc)
        return rc
    }

    fun makeTRUWithABunchOfFakes(user: User = DEFAULT_ADMIN_USER): Pair<TimeRecordingUtilities, FakeRolesChecker>{
        val frc = FakeRolesChecker(CurrentUser(user))
        val tru = TimeRecordingUtilities(FakeTimeEntryPersistence(), CurrentUser(user), testLogger, frc)
        return Pair(tru, frc)
    }
}