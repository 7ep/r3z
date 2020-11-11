package coverosR3z.authentication

import coverosR3z.*
import coverosR3z.domainobjects.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.IllegalArgumentException

class AuthenticationUtilitiesTests {
    lateinit var authUtils : AuthenticationUtilities
    lateinit var ap : FakeAuthPersistence

    @Before
    fun init() {
        ap = FakeAuthPersistence()
        authUtils = AuthenticationUtilities(ap)
    }

    @Test
    fun `It should not be possible to register a new user with an empty password`() {
        val result = authUtils.analyzePassword("")

        assertEquals("the result should clearly indicate an empty password", RegistrationPasswordResult.EMPTY_PASSWORD, result)
    }

    /****************
     * Length Cases *
     ****************/

    /**
     * At a certain point, a password can be too long.
     */
    @Test
    fun `It should not be possible to create a password longer than 255 characters`() {
        val password = "a".repeat(256)
        assertTrue(password.length == 256)

        val result = authUtils.analyzePassword(password)

        assertEquals(RegistrationPasswordResult.PASSWORD_TOO_LONG, result)
    }

    /**
     * At a certain point, a username can be too long.
     */
    @Test
    fun `It should not be possible to create a username longer than 50 characters`() {
        val username = "a".repeat(51)
        assertTrue(username.length == 51)

        val result = authUtils.analyzeUsername(username)

        assertEquals(RegistrationUsernameResult.USERNAME_TOO_LONG, result)
    }

    @Test
    fun `A 255-character password should succeed`() {
        val password = "a".repeat(255)
        assertTrue(password.length == 255)

        val result = authUtils.analyzePassword(password)

        assertEquals(RegistrationPasswordResult.SUCCESS, result)
    }


    @Test
    fun `A 50-character username should succeed`() {
        val username = "a".repeat(50)
        assertTrue(username.length == 50)

        val result = authUtils.analyzeUsername(username)

        assertEquals(RegistrationUsernameResult.SUCCESS, result)
    }

    /**
     * At a certain point, a password can be too short. Under 12 is probably abysmal.
     */
    @Test
    fun `A 11 character password should fail`() {
        val password = "a".repeat(11)
        assertTrue(password.length == 11)

        val result = authUtils.analyzePassword(password)

        assertEquals(RegistrationPasswordResult.PASSWORD_TOO_SHORT, result)
    }

    /**
     * For sanity's sake, let's say that 2 characters is too short for a username
     */
    @Test
    fun `A 2-character username should be considered too short`() {
        val username = "aa"

        val result = authUtils.analyzeUsername(username)

        assertEquals(RegistrationUsernameResult.USERNAME_TOO_SHORT, result)
    }

    /**
     * Three-character usernames would be ok - maybe initials
     */
    @Test
    fun `A 3-character username should be considered ok`() {
        val username = "aaa"

        val result = authUtils.analyzeUsername(username)

        assertEquals(RegistrationUsernameResult.SUCCESS, result)
    }

    @Test
    fun `An 12 character password is a-ok`() {
        val password = "a".repeat(12)
        assertTrue(password.length == 12)

        val result = authUtils.analyzePassword(password)

        assertEquals(RegistrationPasswordResult.SUCCESS, result)
    }

    @Test
    fun `A password greater than 12 chars should pass`() {
        val password = "a".repeat(13)

        val result = authUtils.analyzePassword(password)

        assertEquals(RegistrationPasswordResult.SUCCESS, result)
    }

    @Test
    fun `An account should not be created if the user already exists`() {
        ap.isUserRegisteredBehavior = {true}

        val result = authUtils.analyzeUsername(DEFAULT_USER.name)

        assertEquals(RegistrationUsernameResult.USERNAME_ALREADY_REGISTERED, result)
    }

    /**
     * Say we have "password123", we should get what we know unsalted sha-256 hashes that as
     */
    @Test
    fun `should create a cryptographically secure hash from a password`() {
        val result = Hash.createHash(DEFAULT_PASSWORD)
        assertEquals(DEFAULT_PASSWORD_HASH, result.value)
    }

    @Test
    fun `Should throw exception if we pass in an empty string`() {
        val thrown = assertThrows(IllegalArgumentException::class.java) { authUtils.isUserRegistered("") }
        assertEquals("no username was provided to check", thrown.message)
    }

    @Test
    fun `Should throw exception if we pass in all whitespace`() {
        val thrown = assertThrows(IllegalArgumentException::class.java) { authUtils.isUserRegistered("   ") }
        assertEquals("no username was provided to check", thrown.message)
    }

    /**
     * Cursory tests to work out the functionality of getSalt
     */
    @Test
    fun `password hash should be salted`() {
        val first = Hash.createHash(DEFAULT_PASSWORD)
        val salt : String = Hash.getSalt()
        val second = Hash.createHash(DEFAULT_PASSWORD + salt)
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
        val wellSeasoned = "$DEFAULT_PASSWORD$DEFAULT_SALT"
        ap.getUserBehavior= { User(1, DEFAULT_USER.name, Hash.createHash(wellSeasoned), DEFAULT_SALT, null) }
        val (status, _) = authUtils.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        assertEquals(LoginResult.SUCCESS, status)
    }

    /**
     * I should get a failure status if I log in with the wrong password
     */
    @Test
    fun `should get failure with wrong password`() {
        val wellSeasoned = "$DEFAULT_PASSWORD$DEFAULT_SALT"
        ap.getUserBehavior = { User(1, DEFAULT_USER.name, Hash.createHash(wellSeasoned), DEFAULT_SALT, null) }
        val (status, _) = authUtils.login(DEFAULT_USER.name, "wrong")
        assertEquals(LoginResult.FAILURE, status)
    }


    /**
     * I should get an error telling me my user doesn't exist if I log in with an unregistered user
     */
    @Test
    fun `should get descriptive failure with nonreal user`() {
        ap.getUserBehavior = { NO_USER }
        val (status, _) = authUtils.login(DEFAULT_USER.name, "arbitrary")
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