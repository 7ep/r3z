package coverosR3z.authentication

import coverosR3z.domainobjects.NO_USER
import coverosR3z.domainobjects.SYSTEM_USER
import coverosR3z.server.AnalyzedHttpData
import coverosR3z.server.StatusCode
import coverosR3z.server.Verb
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LogoutAPITests {

    lateinit var au : IAuthenticationUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
    }

    /**
     * Simply checking we get a 200 SUCCESS
     * if we succeed here
     */
    @Test
    fun testShouldLogoutIfAuthenticated() {
        val response = doGETLogout(au, AnalyzedHttpData(Verb.GET, "", emptyMap(), SYSTEM_USER, "", emptyList(), null))
        assertEquals(StatusCode.OK, response.statusCode)
    }

    /**
     * If the user isn't even logged in when they
     * hit this page, they should simply redirect
     * to the homepage
     */
    @Test
    fun testShouldReturnRedirectIfNotAuthenticated() {
        val response = doGETLogout(au, AnalyzedHttpData(Verb.GET, "", emptyMap(), NO_USER, "", emptyList(), null))
        assertEquals(StatusCode.SEE_OTHER, response.statusCode)
    }
}