package coverosR3z.authentication

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.config.LENGTH_OF_BYTES_OF_SESSION_STRING
import coverosR3z.misc.*
import coverosR3z.misc.utility.getTime
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.persistence.utility.PureMemoryDatabase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

class AuthenticationUtilitiesTests {
    private lateinit var authUtils : AuthenticationUtilities
    private lateinit var ap : FakeAuthPersistence

    @Before
    fun init() {
        ap = FakeAuthPersistence()
        authUtils = AuthenticationUtilities(ap, testLogger)
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
        val ex = assertThrows(IllegalArgumentException::class.java){ UserName("a".repeat(51)) }
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
        val ex = assertThrows(IllegalArgumentException::class.java){ UserName("aa") }
        assertEquals(tooSmallUsernameMsg, ex.message)
    }

    @Test
    fun `An empty username should be indicated as such`() {
        val ex = assertThrows(IllegalArgumentException::class.java){ UserName("") }
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

        assertEquals(RegistrationResultStatus.USERNAME_ALREADY_REGISTERED, result.status)
    }

    @Test
    fun `should create a cryptographically secure hash from a password`() {
        assertEquals(DEFAULT_HASH_STRING, DEFAULT_HASH.value)
    }

    @Test
    fun `two different passwords should create different hashes`() {
        val result2 = Hash.createHash(Password(DEFAULT_PASSWORD.value + "a"), DEFAULT_SALT)
        assertNotEquals(DEFAULT_HASH, result2)
    }

    @Test
    fun `two large different passwords should create different hashes`() {
        val result = Hash.createHash(Password("a".repeat(200)), DEFAULT_SALT)
        val result2 = Hash.createHash(Password("a".repeat(201)), DEFAULT_SALT)
        assertNotEquals(result, result2)
    }

    @Test
    fun `two large equal passwords should create equal hashes`() {
        val result = Hash.createHash(Password("a".repeat(200)), DEFAULT_SALT)
        val result2 = Hash.createHash(Password("a".repeat(200)), DEFAULT_SALT)
        assertEquals(result, result2)
    }

    @Test
    fun `two small different passwords should create different hashes`() {
        val result = Hash.createHash(Password("a".repeat(12)), DEFAULT_SALT)
        val result2 = Hash.createHash(Password("a".repeat(13)), DEFAULT_SALT)
        assertNotEquals(result, result2)
    }

    @Test
    fun `two small equal passwords should create equal hashes`() {
        val result = Hash.createHash(Password("a".repeat(12)), DEFAULT_SALT)
        val result2 = Hash.createHash(Password("a".repeat(12)), DEFAULT_SALT)
        assertEquals(result, result2)
    }

    @Test
    fun `two small different passwords with small salts should create different hashes`() {
        val result = Hash.createHash(Password("a".repeat(12)), Salt("b"))
        val result2 = Hash.createHash(Password("a".repeat(13)), Salt("b"))
        assertNotEquals(result, result2)
    }

    /**
     * Contrary to my typical sensibilities, this algorithm we're using for hashing,
     * (see [Hash.createHash]), goes slowly for security reasons.  According to my
     * research, by requiring a slow algorithm, an attacker would be very slowed down
     * in their attempt to brute force their way in.
     *
     * In this test we are documenting that slowness.
     */
    @Test
    fun `test I guess this hash algorithm running slowly is part of the appeal`() {
        val maxMillisAllowed = 1000
        val (time, _) = getTime{
            repeat(5) {
                DEFAULT_HASH
            }
        }
        assertTrue("We would like to see this run in less than $maxMillisAllowed millis, it took $time", time < maxMillisAllowed)
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
        val first = DEFAULT_HASH
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
        ap.getUserBehavior= { User(UserId(1), DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, null) }
        val (status, _) = authUtils.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        assertEquals(LoginResult.SUCCESS, status)
    }

    /**
     * I should get a failure status if I log in with the wrong password
     */
    @Test
    fun `should get failure with wrong password`() {
        ap.getUserBehavior = { User(UserId(1), DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, null) }
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
        assertEquals(LENGTH_OF_BYTES_OF_SESSION_STRING * 2, newSessionId.length)
    }

    /**
     * If a user logs out, we should find all their sessions
     * in the sessions table and wipe them out
     *
     * Also test this is getting persisted to disk.  To do that,
     * we will check that we got what we expected, then reload
     * the database and confirm we get the same results
     */
    @IntegrationTest(usesDirectory = true)
    @Test
    fun testShouldClearAllSessionsWhenLogout() {
        val dbDirectory = DEFAULT_DB_DIRECTORY + "testShouldClearAllSessionsWhenLogout/"
        File(dbDirectory).deleteRecursively()
        val pmd = DatabaseDiskPersistence(dbDirectory, testLogger).startWithDiskPersistence()
        val authPersistence = AuthenticationPersistence(pmd, testLogger)
        val au = AuthenticationUtilities(authPersistence, testLogger)

        // we have to register users so reloading the data from disk works
        val (_, user1) = au.register(DEFAULT_USER.name, DEFAULT_PASSWORD)
        val (_, user2) = au.register(DEFAULT_USER_2.name, DEFAULT_PASSWORD)

        au.createNewSession(user1, DEFAULT_DATETIME) { "abc" }
        au.createNewSession(user1, DEFAULT_DATETIME) { "def" }
        au.createNewSession(user2, DEFAULT_DATETIME) { "ghi" }

        // wipe out all the sessions for this user
        au.logout(user1)

        // check that user1 lacks sessions and user2 still has theirs
        assertTrue(authPersistence.getAllSessions().none{it.user == user1})
        assertEquals(1, authPersistence.getAllSessions().filter {it.user == user2}.size)
        pmd.stop()

        // test out loading it from the disk
        val pmd2 = DatabaseDiskPersistence(dbDirectory = dbDirectory, logger = testLogger).startWithDiskPersistence()
        val authPersistence2 = AuthenticationPersistence(pmd2, testLogger)
        assertTrue(authPersistence2.getAllSessions().none{it.user == user1})
        assertEquals(1, authPersistence2.getAllSessions().filter {it.user == user2}.size)
    }

    /**
     * If somehow the user were to able to attempt to logout
     * while already logged out, an exception should be thrown
     */
    @Test
    fun testShouldFailDeletingSessionsIfAlreadyLoggedOut() {
        val pmd = PureMemoryDatabase()
        val au = AuthenticationUtilities(AuthenticationPersistence(pmd, testLogger), testLogger)

        val ex = assertThrows(IllegalStateException::class.java) { au.logout(DEFAULT_USER) }
        assertEquals("There must exist a session in the database for (${DEFAULT_USER.name.value}) in order to delete it", ex.message)
    }

}