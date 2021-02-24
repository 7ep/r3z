package coverosR3z.authentication

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.misc.*
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
    fun employeeCanDoEverythingAnEmployeeShouldDo() {
        val standardUser = DEFAULT_USER
        authUtils = AuthenticationUtilities(ap, testLogger, CurrentUser(standardUser))
        authUtils.createNewSession(standardUser)
        val tru = createTimeRecordingUtility(standardUser)
    }
}