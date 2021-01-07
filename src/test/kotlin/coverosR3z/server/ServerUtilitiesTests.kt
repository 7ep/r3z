package coverosR3z.server

import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.server.exceptions.DuplicateInputsException
import coverosR3z.misc.utility.getTime
import coverosR3z.server.types.NamedPaths
import coverosR3z.server.types.Verb
import coverosR3z.server.utility.*
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ServerUtilitiesTests {

    private lateinit var au : FakeAuthenticationUtilities
    private lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
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
        var input = "project_entry0=projecta&time_entry0=2&detail_entry0=nothing+to+say"
        for (i in 1..1000) {
            input +=
            "&project_entry$i=projecta&time_entry$i=2&detail_entry$i=nothing+to+say"
        }

        val (time, _) = getTime { parsePostedData(input) }

        val maxTime = 100
        assertTrue("time taken was $time, should be less than $maxTime", time < maxTime)
    }

    /**
     * Following our pattern of failing loud and fast, we want to ensure
     * that if we send garbage to this, it yells at us
     */
    @Test
    fun testShouldHandleBadInputBadly_EmptyString() {
        val ex = assertThrows(IllegalArgumentException::class.java) { parsePostedData("") }

        assertEquals("The POST body was empty", ex.message)
    }

    /**
     * Following our pattern of failing loud and fast, we want to ensure
     * that if we send garbage to this, it yells at us
     */
    @Test
    fun testShouldHandleBadInputBadly_InvalidFormat() {
        // input cannot be split at all
        val input = "foo"
        val ex = assertThrows(IllegalStateException::class.java) { parsePostedData(input) }

        assertEquals("Splitting on = should return 2 values.  Input was foo", ex.message)
    }

    /**
     * What if someone posts a huge name? Like Henry the Eighth I am I am, Henry the Eighth I am!
     */
    @Test
    fun testShouldParseBigName() {
        val input = "username=Henry+the+Eighth+I+am+I+am%2C+Henry+the+Eighth+I+am%21&password=l%21Mfr%7EWc9gIz%27pbXs7%5B%5Dl%7C%27lBM4%2FNg3t8nYevRUNQcL_%2BSW%25A522sThETaQlbB%5E%7BqiNJWzpblP%6024N_V8A6%23A-2T%234%7Dc%29DP%25%3Bm1WC_RXlI%7DMyZHo7*Q1%28kC%2BlC%2F9%28%27%2BjMA9%2Ffr%24IZ%2C%5C5%3DBivXp36tb&employee=1"
        val expected = mapOf("username" to "Henry the Eighth I am I am, Henry the Eighth I am!", "password" to "l!Mfr~Wc9gIz'pbXs7[]l|'lBM4/Ng3t8nYevRUNQcL_+SW%A522sThETaQlbB^{qiNJWzpblP`24N_V8A6#A-2T#4}c)DP%;m1WC_RXlI}MyZHo7*Q1(kC+lC/9('+jMA9/fr\$IZ,\\5=BivXp36tb", "employee" to "1")

        val result = parsePostedData(input)

        assertEquals(expected, result)
    }

    /**
     * What if we need to parse more exotic Unicode chars?
     */
    @Test
    fun testShouldParseUnicodeSymbols() {
        val input = "username=%09%21%22%23%24%25%26%27%28%29*%2B%2C-.%2FA0123456789A%3A%3B%3C%3D%3E%3F%40UABCDEFGHIJKLMNOPQRSTUVWXYZA%5B%5C%5D%5E_%60LabcdefghijklmnopqrstuvwxyzA%7B%7C%7D%7ECL%C2%A1%C2%A2%C2%A3%C2%A4%C2%A5%C2%A6%C2%A7%C2%A8%C2%A9%C2%AA%C2%AB%C2%AC%C2%AE%C2%AF%C2%B0%C2%B1%C2%B2%C2%B3%C2%B4%C2%B5%C2%B6%C2%B7%C2%B8%C2%B9%C2%BA%C2%BB%C2%BC%C2%BD%C2%BE%C2%BFL%C3%80%C3%81%C3%82%C3%83%C3%84%C3%85%C3%86%C3%87%C3%88%C3%89%C3%8A%C3%8B%C3%8C%C3%8D%C3%8E%C3%8F%C3%90%C3%91%C3%92%C3%93%C3%94%C3%95%C3%96M%C3%97L%C3%98%C3%99%C3%9A%C3%9B%C3%9C%C3%9D%C3%9E%C3%9F%C3%A0%C3%A1%C3%A2%C3%A3%C3%A4%C3%A5%C3%A6%C3%A7%C3%A8%C3%A9%C3%AA%C3%AB%C3%AC%C3%AD%C3%AE%C3%AF%C3%B0%C3%B1%C3%B2%C3%B3%C3%B4%C3%B5%C3%B6M%C3%B7L%C3%B8%C3%B9%C3%BA%C3%BB%C3%BC%C3%BD%C3%BE%C3%BFE%C5%81%C5%82%C5%83%C5%84%C5%85%C5%86%C5%87%C5%88E%C5%8A%C5%8B%C5%8C%C5%8D%C5%8E%C5%8F%C5%90%C5%91%C5%92%C5%93%C5%94%C5%95%C5%96%C5%97%C5%98%C5%99%C5%9A%C5%9B%C5%9C%C5%9D%C5%9E%C5%9F%C5%A0%C5%A1%C5%A2%C5%A3%C5%A4%C5%A5%C5%A6%C5%A7%C5%A8%C5%A9%C5%AA%C5%AB%C5%AC%C5%AD%C5%AE%C5%AF%C5%B0%C5%B1%C5%B4%C5%B5%C5%B6%C5%B7%C5%B8%C5%B9%C5%BA%C5%BB%C5%BC%C5%BD%C5%BE%C5%BF"
        val expected = mapOf("username" to "\t!\"#\$%&'()*+,-./A0123456789A:;<=>?@UABCDEFGHIJKLMNOPQRSTUVWXYZA[\\]^_`LabcdefghijklmnopqrstuvwxyzA{|}~CL¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿LÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖM×LØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöM÷LøùúûüýþÿEŁłŃńŅņŇňEŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŴŵŶŷŸŹźŻżŽžſ")

        val result = parsePostedData(input)

        assertEquals(expected, result)
    }

    /**
     * Like [testShouldParseUnicodeSymbols] but on a smaller scale
     * to help focus
     */
    @Test
    fun testShouldParseSmallUnicodeInput() {
        val input = "username=%20%C2%A1"
        val expected = mapOf("username" to " ¡")

        val result = parsePostedData(input)

        assertEquals(expected, result)
    }

    /**
     * Should throw an exception if we receive multiple
     * items with the same name, e.g. foo=123 and foo=abc
     */
    @Test
    fun testShouldParse_FailWhenDuplicates() {
        val input = "foo=abc&foo=123"

        val ex = assertThrows(DuplicateInputsException::class.java) { parsePostedData(input) }
        assertEquals("foo was duplicated in the post body - had values of abc and 123", ex.message)
    }

    /**
     * Action for a POST to an endpoint
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_POST() {
        val input = serverStatusLineRegex.matchEntire("POST /${NamedPaths.ENTER_TIME.path} HTTP/1.1")
        val expected = Pair(Verb.POST, NamedPaths.ENTER_TIME.path)

        val result = parseStatusLineAsServer(input!!)

        assertEquals(expected, result)
    }

    /**
     * Action for a valid GET on a simple file
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_GET() {
        val input = serverStatusLineRegex.matchEntire("GET /test HTTP/1.1")
        val expected = Pair(Verb.GET, "test")

        val result = parseStatusLineAsServer(input!!)

        assertEquals(expected, result)
    }

    /**
     * Action for a valid GET on a template file
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_TemplateGET() {
        val input = serverStatusLineRegex.matchEntire("GET /test.utl HTTP/1.1")
        val expected = Pair(Verb.GET, "test.utl")

        val result = parseStatusLineAsServer(input!!)

        assertEquals(expected, result)
    }

    /**
     * Action for a valid GET on a css file
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_CSS() {
        val input = serverStatusLineRegex.matchEntire("GET /test.css HTTP/1.1")
        val expected = Pair(Verb.GET, "test.css")

        val result = parseStatusLineAsServer(input!!)

        assertEquals(expected, result)
    }

    /**
     * Action for a valid GET on a JavaScript file
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_JS() {
        val input = serverStatusLineRegex.matchEntire("GET /test.js HTTP/1.1")
        val expected = Pair(Verb.GET, "test.js")

        val result = parseStatusLineAsServer(input!!)

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
     * Especially when using unicode, the content-length can turn
     * out to be really high, like double or more the real size of text
     * because it gets encoded.  Setting it to 10,000
     */
    @Test
    fun testShouldExtractLengthFromContentLength_TooHigh() {
        val headers = listOf("Content-Length: ${maxContentLength +1}", "Content-Type: Blah")

        val exception = assertThrows(Exception::class.java) { extractLengthOfPostBodyFromHeaders(headers) }
        assertEquals("Exception occurred for these headers: Content-Length: ${maxContentLength +1};Content-Type: Blah.  Inner exception message: The Content-length is not allowed to exceed $maxContentLength characters", exception.message)
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
        val user = extractUserFromAuthToken(authCookie, au)
        assertEquals("we should find a particular user mapped to this session id", expectedUser, user)
    }

}