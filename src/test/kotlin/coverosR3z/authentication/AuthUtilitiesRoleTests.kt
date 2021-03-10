package coverosR3z.authentication

import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.authentication.utility.FakeRolesChecker
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.misc.*
import org.junit.Assert.*
import org.junit.Test

class AuthUtilitiesRoleTests {


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
        val (au, frc) = makeAuthUtils(DEFAULT_REGULAR_USER)

        au.addRoleToUser(DEFAULT_REGULAR_USER, Roles.ADMIN)
        assertFalse(frc.roleCanDoAction)

        au.createNewSession(DEFAULT_USER, DEFAULT_DATETIME)
        assertFalse(frc.roleCanDoAction)

        au.getUserForSession(DEFAULT_SESSION_TOKEN)
        assertFalse(frc.roleCanDoAction)

        au.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        assertFalse(frc.roleCanDoAction)

        au.logout(DEFAULT_USER)
        assertFalse(frc.roleCanDoAction)

        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD, DEFAULT_EMPLOYEE.id)
        assertFalse(frc.roleCanDoAction)
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
        val (au, frc) = makeAuthUtils(DEFAULT_ADMIN_USER)

        au.addRoleToUser(DEFAULT_REGULAR_USER, Roles.ADMIN)
        assertTrue(frc.roleCanDoAction)

        au.createNewSession(DEFAULT_USER, DEFAULT_DATETIME)
        assertFalse(frc.roleCanDoAction)

        au.getUserForSession(DEFAULT_SESSION_TOKEN)
        assertFalse(frc.roleCanDoAction)

        au.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        assertFalse(frc.roleCanDoAction)

        au.logout(DEFAULT_USER)
        assertFalse(frc.roleCanDoAction)

        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD, DEFAULT_EMPLOYEE.id)
        assertFalse(frc.roleCanDoAction)
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
        val (au, frc) = makeAuthUtils(SYSTEM_USER)

        au.addRoleToUser(DEFAULT_REGULAR_USER, Roles.ADMIN)
        assertTrue(frc.roleCanDoAction)

        au.createNewSession(DEFAULT_USER, DEFAULT_DATETIME)
        assertTrue(frc.roleCanDoAction)

        au.getUserForSession(DEFAULT_SESSION_TOKEN)
        assertTrue(frc.roleCanDoAction)

        au.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        assertTrue(frc.roleCanDoAction)

        au.logout(DEFAULT_USER)
        assertTrue(frc.roleCanDoAction)

        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD, DEFAULT_EMPLOYEE.id)
        assertTrue(frc.roleCanDoAction)
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
        val (au, frc) = makeAuthUtils(DEFAULT_APPROVER)

        au.addRoleToUser(DEFAULT_REGULAR_USER, Roles.ADMIN)
        assertFalse(frc.roleCanDoAction)

        au.createNewSession(DEFAULT_USER, DEFAULT_DATETIME)
        assertFalse(frc.roleCanDoAction)

        au.getUserForSession(DEFAULT_SESSION_TOKEN)
        assertFalse(frc.roleCanDoAction)

        au.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        assertFalse(frc.roleCanDoAction)

        au.logout(DEFAULT_USER)
        assertFalse(frc.roleCanDoAction)

        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD, DEFAULT_EMPLOYEE.id)
        assertFalse(frc.roleCanDoAction)
    }

    /*
                              _       _          _
  _ _  ___ _ _  ___   _ _ ___| |___  | |_ ___ __| |_ ___
 | ' \/ _ \ ' \/ -_) | '_/ _ \ / -_) |  _/ -_|_-<  _(_-<
 |_||_\___/_||_\___| |_| \___/_\___|  \__\___/__/\__/__/

     */
    @Test
    fun testNoneRole() {
        val (au, frc) = makeAuthUtils(NO_USER)

        au.addRoleToUser(DEFAULT_REGULAR_USER, Roles.ADMIN)
        assertFalse(frc.roleCanDoAction)

        au.createNewSession(DEFAULT_USER, DEFAULT_DATETIME)
        assertFalse(frc.roleCanDoAction)

        au.getUserForSession(DEFAULT_SESSION_TOKEN)
        assertFalse(frc.roleCanDoAction)

        au.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        assertFalse(frc.roleCanDoAction)

        au.logout(DEFAULT_USER)
        assertFalse(frc.roleCanDoAction)

        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD, DEFAULT_EMPLOYEE.id)
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

    private fun makeAuthUtils(user: User): Pair<IAuthenticationUtilities, FakeRolesChecker> {
        val cu = CurrentUser(user)
        val rc = FakeRolesChecker(cu)
        val ap = FakeAuthPersistence()
        val authUtils = AuthenticationUtilities(ap, testLogger, cu, rc)
        return Pair(authUtils, rc)
    }
}