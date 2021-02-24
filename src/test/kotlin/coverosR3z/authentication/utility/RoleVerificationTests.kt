package coverosR3z.authentication.utility

import coverosR3z.authentication.FakeAuthPersistence
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.misc.*
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class RoleVerificationTests {

    private lateinit var authUtils : AuthenticationUtilities
    private lateinit var ap : FakeAuthPersistence

    @Before
    fun init() {
        ap = FakeAuthPersistence()
//        authUtils = AuthenticationUtilities(ap, testLogger, CurrentUser(DEFAULT_ADMIN_USER))
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

    @Test
    fun regularRoleCannotCreateSession() {
        val rc = makeAuthUtils(CurrentUser(DEFAULT_USER))
        authUtils.createNewSession(DEFAULT_USER)
        assertFalse(rc.didAuthorize)
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
}