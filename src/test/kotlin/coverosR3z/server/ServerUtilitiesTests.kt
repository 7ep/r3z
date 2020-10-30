package coverosR3z.server

import coverosR3z.getTime
import coverosR3z.server.ServerUtilities.Companion.Action
import coverosR3z.server.ServerUtilities.Companion.ActionType
import coverosR3z.server.ServerUtilities.Companion.parseClientRequest
import coverosR3z.server.ServerUtilities.Companion.parsePostedData
import org.junit.Assert.*
import org.junit.Test
import java.lang.IllegalArgumentException

class ServerUtilitiesTests {

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



    @Test
    fun testShouldParseMultipleClientRequestTypes() {
        val input = "POST /entertime HTTP/1.1"
        val expected = Action(ActionType.HANDLE_POST_FROM_CLIENT, "")

        val result = parseClientRequest(input)

        assertEquals(expected, result)
    }
}