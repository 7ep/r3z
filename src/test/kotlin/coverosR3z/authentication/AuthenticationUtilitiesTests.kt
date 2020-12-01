package coverosR3z.authentication

import coverosR3z.*
import coverosR3z.domainobjects.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.IllegalArgumentException

class AuthenticationUtilitiesTests {
    private lateinit var authUtils : AuthenticationUtilities
    private lateinit var ap : FakeAuthPersistence

    @Before
    fun init() {
        ap = FakeAuthPersistence()
        authUtils = AuthenticationUtilities(ap)
    }

    @Test
    fun `It should not be possible to register a new user with an empty password`() {
        val ex = assertThrows(IllegalArgumentException::class.java){Password("")}

        assertEquals(passwordMustNotBeBlankMsg, ex.message)
    }

    /****************
     * Length Cases *
     ****************/

    /**
     * At a certain point, a password can be too long.
     */
    @Test
    fun `It should not be possible to create a password longer than 255 characters`() {
        val ex = assertThrows(IllegalArgumentException::class.java){Password("a".repeat(256))}
        assertEquals(passwordMustNotBeTooLargeMsg, ex.message)
    }

    /**
     * At a certain point, a username can be too long.
     */
    @Test
    fun `It should not be possible to create a username longer than 50 characters`() {
        val ex = assertThrows(IllegalArgumentException::class.java){UserName("a".repeat(51))}
        assertEquals(tooLargeUsernameMsg, ex.message)
    }

    @Test
    fun `A 255-character password should succeed`() {
        val password = Password("a".repeat(255))
        assertEquals(password.value, "a".repeat(255))
    }


    @Test
    fun `A 50-character username should succeed`() {
        val username = UserName("a".repeat(50))

        assertEquals(username.value, "a".repeat(50))
    }

    /**
     * At a certain point, a password can be too short. Under 12 is probably abysmal.
     */
    @Test
    fun `A 11 character password should fail`() {
        val ex = assertThrows(IllegalArgumentException::class.java){Password("a".repeat(11))}
        assertEquals(passwordMustBeLargeEnoughMsg, ex.message)
    }

    /**
     * For sanity's sake, let's say that 2 characters is too short for a username
     */
    @Test
    fun `A 2-character username should be considered too short`() {
        val ex = assertThrows(IllegalArgumentException::class.java){UserName("aa")}
        assertEquals(tooSmallUsernameMsg, ex.message)
    }

    @Test
    fun `An empty username should be indicated as such`() {
        val ex = assertThrows(IllegalArgumentException::class.java){UserName("")}
        assertEquals(usernameCannotBeEmptyMsg, ex.message)
    }

    /**
     * Three-character usernames would be ok - maybe initials
     */
    @Test
    fun `A 3-character username should be considered ok`() {
        val username = UserName("aaa")

        assertEquals(username.value, "aaa")
    }

    @Test
    fun `An 12 character password is a-ok`() {
        val password = Password("a".repeat(12))

        assertEquals(password.value, "a".repeat(12))
    }

    @Test
    fun `A password greater than 12 chars should pass`() {
        val password = Password("a".repeat(13))

        assertEquals(password.value, "a".repeat(13))
    }

    @Test
    fun `An account should not be created if the user already exists`() {
        ap.isUserRegisteredBehavior = {true}

        val result = authUtils.register(DEFAULT_USER.name, DEFAULT_PASSWORD, null)

        assertEquals(RegistrationResult.USERNAME_ALREADY_REGISTERED, result)
    }

    /**
     * Say we have "password123", we should get what we know unsalted sha-256 hashes that as
     */
    @Test
    fun `should create a cryptographically secure hash from a password`() {
        val result = Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT)
        assertEquals(DEFAULT_PASSWORD_HASH, result.value)
    }

    @Test
    fun `Should throw exception if we pass in an empty string`() {
        val thrown = assertThrows(IllegalArgumentException::class.java) { authUtils.isUserRegistered(UserName("")) }
        assertEquals(usernameCannotBeEmptyMsg, thrown.message)
    }

    @Test
    fun `Should throw exception if we pass in all whitespace`() {
        val thrown = assertThrows(IllegalArgumentException::class.java) { authUtils.isUserRegistered(UserName("   ")) }
        assertEquals(usernameCannotBeEmptyMsg, thrown.message)
    }

    /**
     * Cursory tests to work out the functionality of getSalt
     */
    @Test
    fun `two salts should give differeing hashes`() {
        val first = Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT)
        val salt = Hash.getSalt()
        val second = Hash.createHash(DEFAULT_PASSWORD, salt)
        assertNotEquals(first, second)
    }

    /**
     *The intention of this is to ensure that the getSalt method is implemented in such a way
     * that it provides randomness. Nothing in this test actually ensures secure randomness.
     */
    @Test
    fun `salts should not be the same each time`(){
        val first = Hash.getSalt()
        val second = Hash.getSalt()
        assertNotEquals(first, second)
    }

    /**
     * I should get a success status if I log in with valid credentials
     */
    @Test
    fun `should get success with valid login`() {
        ap.getUserBehavior= { User(UserId(1), DEFAULT_USER.name, Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT), DEFAULT_SALT, null) }
        val (status, _) = authUtils.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        assertEquals(LoginResult.SUCCESS, status)
    }

    /**
     * I should get a failure status if I log in with the wrong password
     */
    @Test
    fun `should get failure with wrong password`() {
        ap.getUserBehavior = { User(UserId(1), DEFAULT_USER.name, Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT), DEFAULT_SALT, null) }
        val (status, _) = authUtils.login(DEFAULT_USER.name, Password("wrongwrongwrong"))
        assertEquals(LoginResult.FAILURE, status)
    }


    /**
     * I should get an error telling me my user doesn't exist if I log in with an unregistered user
     */
    @Test
    fun `should get descriptive failure with nonreal user`() {
        ap.getUserBehavior = { NO_USER }
        val (status, _) = authUtils.login(DEFAULT_USER.name, Password("arbitraryarbitrary"))
        assertEquals(LoginResult.NOT_REGISTERED, status)
    }

    /**
     * We will give the user a token - a string - they can use to
     * instantly confirm they are authenticated with us.
     *
     * They hold this value in the cookie we hand them.
     */
    @Test
    fun testShouldConfirmUserAuthenticationBySessionToken() {
        ap.getUserForSessionBehavior = { DEFAULT_USER }
        assertEquals(DEFAULT_USER, authUtils.getUserForSession(DEFAULT_SESSION_TOKEN))
    }

    /**
     * Creating a session is just a matter of storing a new
     * entry in the sessions data structure, with the session
     * identifier - a randomly generated string - and the user
     * who is considered authenticated.
     */
    @Test
    fun testShouldCreateNewSession() {
        val newSessionId = authUtils.createNewSession(DEFAULT_USER)
        assertEquals(32, newSessionId.length)
    }

}