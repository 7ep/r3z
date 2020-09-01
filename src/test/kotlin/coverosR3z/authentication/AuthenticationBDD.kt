package coverosR3z.authentication

import coverosR3z.domainobjects.RegistrationResult
import coverosR3z.persistence.PureMemoryDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 *
 * As a employee,
 * I want to be able to securely use the system,
 * so that I know my time entries are confidential and cannot be manipulated by others
 */
class AuthenticationBDD {

    /**
     * The key here is that when we do this, it's
     * recording who does what, and only allows proper
     * people to carry out actions.  Like, Alice
     * can only modify Alice's stuff.
     */
    @Test
    fun `I can add a time entry`() {
        // given I am logged in
        // when I add a time entry
        // then it proceeds successfully
    }

    @Test
    fun `I cannot change someone else's time`() {
        // given I am logged in as user_a
        // when I try to add a time-entry for user b
        // then the system disallows it
    }

    @Test
    fun `I should be able to register a employee with a valid password`() {
        // given I am not currently registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)

        // when I register a new employee with employeename "matt" and password "asdfoiajwefowejf"
        au.register("matt", "asdfoiajwefowejf")

        // then the system records the registration successfully
        assertTrue("our user should be registered", au.isUserRegistered("matt"))
    }

    @Test
    fun `I should not be able to register a employee if they are already registered`() {
        // given I am currently registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register("matt", "asdfoiajwefowejf")

        // when I register again with the same credentials
        val result = au.register("matt", "asdfoiajwefowejf")

        // then the system records the registration successfully
        assertEquals("we shouldn't be allowed to register a user again", RegistrationResult.ALREADY_REGISTERED, result)
    }

    @Test
    fun `I should be able to log in once I'm a registered employee`() {
        // given I have registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register("matt", "asdfoiajwefowejf")

        // when I enter valid credentials
        val (status, _) = au.login("matt", "asdfoiajwefowejf")

        // then the system knows who I am
//        assertEquals("SUCCESS", status)
        // TODO
    }

    @Test
    fun `if I enter a bad password while logging in, I will be denied access`() {
        // given I have registered using "usera" and "password123"
        // when I login with "usera" and "not_right_password"
        // then the system denies me access
    }

    @Test
    fun `if I enter an invalid password while registering, it will disallow it`() {
        // given I am unregistered
        // when I register with "usera" and "short" as password
        // then the system denies the registration on the basis of a bad password
    }


}
