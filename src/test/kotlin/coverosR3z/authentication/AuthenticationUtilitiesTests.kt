package coverosR3z.authentication

import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.RegistrationResult
import coverosR3z.domainobjects.User
import coverosR3z.domainobjects.UserName
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
        val result = authUtils.register("matt", "")

        assertEquals("the result should clearly indicate an empty password", RegistrationResult.EMPTY_PASSWORD, result)
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
        assert(password.length == 256)

        val result = authUtils.register("matt", password)

        assertEquals(RegistrationResult.PASSWORD_TOO_LONG, result)
    }

    @Test
    fun `A 255-character password should succeed`() {
        val password = "a".repeat(255)
        assert(password.length == 255)

        val result = authUtils.register("matt", password)

        assertEquals(RegistrationResult.SUCCESS, result)
    }

    /**
     * At a certain point, a password can be too short. Under 12 is probably abysmal.
     */
    @Test
    fun `A 11 character password should fail`() {
        val password = "a".repeat(11)
        assert(password.length == 11)

        val result = authUtils.register("matt", password)

        assertEquals(RegistrationResult.PASSWORD_TOO_SHORT, result)
    }

    @Test
    fun `An 12 character password is a-ok`() {
        val password = "a".repeat(12)
        assert(password.length == 12)

        val result = authUtils.register("matt", password)

        assertEquals(RegistrationResult.SUCCESS, result)
    }

    @Test
    fun `A password greater than 12 chars should pass`() {
        val password = "a".repeat(13)

        val result = authUtils.register("matt", password)

        assertEquals(RegistrationResult.SUCCESS, result)
    }

    @Test
    fun `An account should not be created if the user already exists`() {
        ap.isUserRegisteredBehavior = {true}

        val result = authUtils.register("matt", "just don't care")

        assertEquals(RegistrationResult.ALREADY_REGISTERED, result)
    }

    @Test
    fun `Should determine if a particular username is for a registered user`() {
        ap = FakeAuthPersistence(isUserRegisteredBehavior = {true})
        authUtils = AuthenticationUtilities(ap)

        val result = authUtils.isUserRegistered("jenna")
        assertEquals(true, result)
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

    fun createUser() {
        val salt = "blahblahblah"
        val testUser = User(1, "matt", Hash.createHash("password123" + salt), salt)
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

}