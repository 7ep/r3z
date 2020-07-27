package coverosR3z.authentication

import coverosR3z.domainobjects.RegistrationResult
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

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

}