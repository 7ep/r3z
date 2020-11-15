package coverosR3z.server

import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.getTime
import coverosR3z.server.ServerUtilities.Companion.extractSessionTokenFromHeaders
import coverosR3z.server.ServerUtilities.Companion.extractLengthOfPostBodyFromHeaders
import coverosR3z.server.ServerUtilities.Companion.parseFirstLine
import coverosR3z.server.ServerUtilities.Companion.parsePostedData
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.Exception
import java.lang.IllegalArgumentException

class ServerUtilitiesTests {

    private lateinit var au : FakeAuthenticationUtilities
    private lateinit var tru : FakeTimeRecordingUtilities
    private lateinit var su : ServerUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
        su = ServerUtilities(au, tru)
    }

    /**
     * When the client POSTs data to us, it's coming
     * in a particular format, called application/x-www-form-urlencoded
     *
     * an example of this would be:
     * project_entry=projecta&time_entry=2&detail_entry=nothing+to+say
     *
     * We should parse this to a map fairly easily.
     */
    @Test
    fun testShouldParseData() {
        val input = "project_entry=projecta&time_entry=2&detail_entry=nothing+to+say"
        val expected = mapOf("project_entry" to "projecta", "time_entry" to "2", "detail_entry" to "nothing to say")

        val result = parsePostedData(input)

        assertEquals(expected, result)
    }

    /**
     * Make sure it doesn't choke on huge inputs
     */
    @Test
    fun testShouldParseData_PERFORMANCE() {
        var input = ""
        for (i in 1..1000) {
            input +=
            "project_entry$i=projecta&time_entry$i=2&detail_entry$i=nothing+to+say"
        }

        val time = getTime {parsePostedData(input)}

        val maxTime = 100
        assertTrue("time taken was ${time.first}, should be less than $maxTime", time.first < maxTime)
    }

    /**
     * Following our pattern of failing loud and fast, we want to ensure
     * that if we send garbage to this, it yells at us
     */
    @Test
    fun testShouldHandleBadInputBadly_EmptyString() {
        val ex = assertThrows(IllegalArgumentException::class.java) { parsePostedData("")}

        assertEquals("The input to parse was empty", ex.message)
    }

    /**
     * Following our pattern of failing loud and fast, we want to ensure
     * that if we send garbage to this, it yells at us
     */
    @Test
    fun testShouldHandleBadInputBadly_InvalidFormat() {
        // input cannot be split at all
        val input = "foo"
        val ex = assertThrows(IllegalArgumentException::class.java) { parsePostedData(input)}

        assertEquals("We failed to parse \"foo\" as application/x-www-form-urlencoded", ex.message)
    }

    /**
     * Action for a POST to an endpoint
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_POST() {
        val input = "POST /${NamedPaths.ENTER_TIME.path} HTTP/1.1"
        val expected = Pair(Verb.POST, NamedPaths.ENTER_TIME.path)

        val result = parseFirstLine(input)

        assertEquals(expected, result)
    }

    /**
     * Action for a valid GET on a simple file
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_GET() {
        val input = "GET /test HTTP/1.1"
        val expected = Pair(Verb.GET, "test")

        val result = parseFirstLine(input)

        assertEquals(expected, result)
    }

    /**
     * Action for a valid GET on a template file
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_TemplateGET() {
        val input = "GET /test.utl HTTP/1.1"
        val expected = Pair(Verb.GET, "test.utl")

        val result = parseFirstLine(input)

        assertEquals(expected, result)
    }

    /**
     * Action for an invalid request
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_BadRequest() {
        val input = "INVALID /test.utl HTTP/1.1"
        val expected = Pair(Verb.INVALID, "")

        val result = parseFirstLine(input)

        assertEquals(expected, result)
    }

    /**
     * Action for a valid GET on a css file
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_CSS() {
        val input = "GET /test.css HTTP/1.1"
        val expected = Pair(Verb.GET, "test.css")

        val result = parseFirstLine(input)

        assertEquals(expected, result)
    }

    /**
     * Action for a valid GET on a JavaScript file
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_JS() {
        val input = "GET /test.js HTTP/1.1"
        val expected = Pair(Verb.GET, "test.js")

        val result = parseFirstLine(input)

        assertEquals(expected, result)
    }

    /**
     * Check to make sure we can extract the auth value
     * from the Cookie header
     */
    @Test
    fun testShouldExtractAuthCodeFromCookie() {
        val headers = listOf("Cookie: jenkins-timestamper-offset=18000000; sessionId=38afes7a8; Idea-7de3a10=8972cd6b-ad6d-40c6-9daf-38ef0f149214; jenkins-timestamper=system; jenkins-timestamper-local=true")
        val expected = "38afes7a8"
        val result = extractSessionTokenFromHeaders(headers)
        assertEquals("we should extract out the auth value from the header provided", expected, result)
    }

    /**
     * check lowercase works also
     */
    @Test
    fun testShouldExtractAuthCodeFromCookie_lowercase() {
        val headers = listOf("cookie: jenkins-timestamper-offset=18000000; sessionId=38afes7a8; Idea-7de3a10=8972cd6b-ad6d-40c6-9daf-38ef0f149214; jenkins-timestamper=system; jenkins-timestamper-local=true")
        val expected = "38afes7a8"
        val result = extractSessionTokenFromHeaders(headers)
        assertEquals("we should extract out the auth value from the header provided", expected, result)
    }

    /**
     * What if we receive multiple Cookie headers? Concatenate!
     */
    @Test
    fun testShouldExtractAuthCodeFromCookie_MultipleCookieHeaders_ShouldConcatenate() {
        val headers = listOf("Cookie: sessionId=38afes7a8", "Cookie: a=b; jenkins-timestamper-offset=18000000")
        val expected = "38afes7a8"
        val result = extractSessionTokenFromHeaders(headers)
        assertEquals("we should extract out the auth value from the header provided", expected, result)
    }

    /**
     * If we don't find a cookie header, no big whoop.
     */
    @Test
    fun testShouldExtractAuthCodeFromCookie_NotFound() {
        val headers = listOf("Content-Type: Blah")
        val result = extractSessionTokenFromHeaders(headers)
        assertNull("If there were no cookie headers, return null", result)
    }

    /**
     * If we find a cookie header, but not our auth cookie (sessionId=???), no big whoop
     */
    @Test
    fun testShouldExtractAuthCodeFromCookie_NotFound_NoSessionCookie() {
        val headers = listOf("Cookie: jenkins-timestamper-offset=18000000; Idea-7de3a10=8972cd6b-ad6d-40c6-9daf-38ef0f149214; jenkins-timestamper=system; jenkins-timestamper-local=true")
        val result = extractSessionTokenFromHeaders(headers)
        assertNull("If there were no cookie headers, return null", result)
    }

    /**
     * Check to make sure we can extract the length of the
     * body of a POST from the Content-Length header it sends
     * us.
     */
    @Test
    fun testShouldExtractLengthFromContentLength() {
        //arrange
        val headers = listOf("Content-Length: 20")
        val expectedLength = 20

        //act
        val result = extractLengthOfPostBodyFromHeaders(headers)

        //assert
        assertEquals("we should extract out the length from the header provided", expectedLength, result)
    }

    /**
     * Make sure it still works with lower-case
     */
    @Test
    fun testShouldExtractLengthFromContentLength_lowercase() {
        //arrange
        val headers = listOf("Content-length: 20")
        val expectedLength = 20

        //act
        val result = extractLengthOfPostBodyFromHeaders(headers)

        //assert
        assertEquals("we should extract out the length from the header provided", expectedLength, result)
    }

    /**
     * Make sure we extract properly with multiple headers
     */
    @Test
    fun testShouldExtractLengthFromContentLength_MultipleHeaders() {
        val headers = listOf("Content-Length: 20", "Content-Type: Blah")
        val expectedLength = 20

        val result = extractLengthOfPostBodyFromHeaders(headers)

        assertEquals("we should extract out the length from the header provided", expectedLength, result)
    }

    /**
     * What if we receive multiple Content-Length headers? Complain!
     */
    @Test
    fun testShouldExtractLengthFromContentLength_MultipleContentLengthHeaders() {
        val headers = listOf("Content-Length: 20", "Content-Length: 15")

        val exception = assertThrows(Exception::class.java) { extractLengthOfPostBodyFromHeaders(headers) }

        assertEquals("Exception occurred for these headers: Content-Length: 20;Content-Length: 15.  Inner exception message: Collection contains more than one matching element.", exception.message)
    }

    /**
     * We'll throw an exception for now if we don't get this
     */
    @Test
    fun testShouldExtractLengthFromContentLength_NotFound() {
        val headers = listOf("Content-Type: Blah")
        val exception = assertThrows(NoSuchElementException::class.java) { extractLengthOfPostBodyFromHeaders(headers) }
        assertEquals("Did not find a necessary Content-Length header in headers. Headers: Content-Type: Blah", exception.message)
    }

    /**
     * We'll throw an exception if we read no headers at all
     */
    @Test
    fun testShouldExtractLengthFromContentLength_NoHeaders() {
        val headers = emptyList<String>()

        val exception = assertThrows(IllegalArgumentException::class.java) { extractLengthOfPostBodyFromHeaders(headers) }
        assertEquals("We must receive at least one header at this point or the request is invalid", exception.message)
    }

    /**
     * If the Content-Length has an unparsable length...
     */
    @Test
    fun testShouldExtractLengthFromContentLength_Unparsable() {
        val headers = listOf("Content-Length: aaaa", "Content-Type: Blah")
        val exception = assertThrows(IllegalArgumentException::class.java) { extractLengthOfPostBodyFromHeaders(headers) }
        assertEquals("The value for content-length was not parsable as an integer. Headers: Content-Length: aaaa;Content-Type: Blah", exception.message)
    }

    /**
     * If the Content-Length has length too high...
     *
     * what *is* the greatest length we should take? Off-hand I'd
     * say it doesn't make sense for us to take large data from a user.
     * Probably no more than 500 characters, for now.
     */
    @Test
    fun testShouldExtractLengthFromContentLength_TooHigh() {
        val headers = listOf("Content-Length: 501", "Content-Type: Blah")

        val exception = assertThrows(Exception::class.java) { extractLengthOfPostBodyFromHeaders(headers) }
        assertEquals("Exception occurred for these headers: Content-Length: 501;Content-Type: Blah.  Inner exception message: The Content-length is not allowed to exceed 500 characters", exception.message)
    }

    /**
     * If the Content-Length has a negative value
     *
     * Content-Length cannot be negative
     */
    @Test
    fun testShouldExtractLengthFromContentLength_Negative() {
        val headers = listOf("Content-Length: -1", "Content-Type: Blah")

        val exception = assertThrows(Exception::class.java) { extractLengthOfPostBodyFromHeaders(headers) }
        assertEquals("Exception occurred for these headers: Content-Length: -1;Content-Type: Blah.  Inner exception message: Content-length cannot be negative", exception.message)
    }

    /**
     * We keep a mapping between users and sessions in the database. It should
     * be easily possible to pass in the session id and get the user.
     */
    @Test
    fun testShouldExtractUserFromAuthToken() {
        val authCookie = "abc123"
        val expectedUser = DEFAULT_USER
        au.getUserForSessionBehavior = { DEFAULT_USER}
        val user = ServerUtilities.extractUserFromAuthToken(authCookie, au)
        assertEquals("we should find a particular user mapped to this session id", expectedUser, user)
    }

}