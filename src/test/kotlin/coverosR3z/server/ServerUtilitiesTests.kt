package coverosR3z.server

import org.junit.Assert.assertEquals
import org.junit.Test

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

        val result : String = parse(input)

        assertEquals(expected, result)
    }

    private fun parse(input: String): String {
        TODO("Not yet implemented")
    }
}