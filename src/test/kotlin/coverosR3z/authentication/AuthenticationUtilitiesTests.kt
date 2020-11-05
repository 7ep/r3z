package coverosR3z.authentication

import coverosR3z.createTimeEntryPreDatabase
import coverosR3z.domainobjects.*
import coverosR3z.timerecording.FakeTimeEntryPersistence
import coverosR3z.timerecording.TimeRecordingUtilities
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

        val result = authUtils.analyzeUsername("matt")

        assertEquals(RegistrationUsernameResult.USERNAME_ALREADY_REGISTERED, result)
    }

    /**
     * Say we have "password123", we should get what we know unsalted sha-256 hashes that as
     */
    @Test
    fun `should create a cryptographically secure hash from a password`() {
        val password = "password123"
        val result = Hash.createHash(password)
        assertEquals("ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f", result.value)
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
        val first = Hash.createHash("password123")
        val salt : String = Hash.getSalt()
        val second = Hash.createHash("password123" + salt)
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
        val salt = Hash.getSalt()
        val wellSeasoned = "password123$salt"
        val fap = FakeAuthPersistence(
                getUserBehavior= { User(1, "matt", Hash.createHash(wellSeasoned), salt, null) }
        )
        val au = AuthenticationUtilities(fap)
        val status = au.login("matt", "password123")
        assertEquals(LoginResult.SUCCESS, status)
    }

    /**
     * I should get a failure status if I log in with the wrong password
     */
    @Test
    fun `should get failure with wrong password`() {

        val salt = Hash.getSalt()
        val wellSeasoned = "password123$salt"
        val fap = FakeAuthPersistence(
                getUserBehavior= { User(1, "matt", Hash.createHash(wellSeasoned), salt, null) }
        )
        val au = AuthenticationUtilities(fap)
        val status= au.login("matt", "wrong")
        assertEquals(LoginResult.FAILURE, status)
    }


    /**
     * I should get an error telling me my user doesn't exist if I log in with an unregistered user
     */
    @Test
    fun `should get descriptive failure with nonreal user`() {
        val fap = FakeAuthPersistence(
                getUserBehavior= { null }
        )
        val au = AuthenticationUtilities(fap)
        val status = au.login("matt", "arbitrary")
        assertEquals(LoginResult.NOT_REGISTERED, status)
    }
}