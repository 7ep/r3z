package coverosR3z.domainobjects

import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import coverosR3z.jsonSerialzation as json

class DateTests {

    private val date = Date(18438)

    // Json also has .Default configuration which provides more reasonable settings,
    // but is subject to change in future versions

    /**
     * Super basic.  Two identical dates should be equal
     */
    @Test
    fun testShouldEqual() {
        val d1 = Date(2020, Month.NOV, 21)
        val d2 = Date(2020, Month.NOV, 21)
        assertEquals(d1, d2)
    }

    /**
     * Super basic.  Two non-identical dates should not be equal
     */
    @Test
    fun testShouldNotEqual() {
        val d1 = Date(2020, Month.NOV, 21)
        val d2 = Date(2020, Month.NOV, 22)
        assertNotEquals(d1, d2)
    }

    @Test
    fun `can serialize Date with Kotlin serialization`() {
        // serializing objects
        val jsonData = json.encodeToString(Date.serializer(), date)
        assertEquals("""{"epochDay":18438}""", jsonData)

        // serializing lists
        val jsonList = json.encodeToString(ListSerializer(Date.serializer()), listOf(date))
        assertEquals("""[{"epochDay":18438}]""", jsonList)

        // parsing data back
        val obj: Date = json.decodeFromString(Date.serializer(), """{"epochDay":18438}""")
        assertEquals(date, obj)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong`() {
        val ex = assertThrows(Exception::class.java) { json.decodeFromString(Date.serializer(), """{"epochDay":18438L,"stringValue":"2020-06-25"}""") }
        assertEquals("Unexpected JSON token at offset 19: Failed to parse 'int'\n" +
                "JSON input: {\"epochDay\":18438L,\"stringValue\":\"2020-06-25\"}", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, too high a year`() {
        val ex = assertThrows(IllegalArgumentException::class.java) { json.decodeFromString(Date.serializer(), """{"epochDay":91438}""") }
        assertEquals("no way on earth people are using this before 2020 or past 2100, you had a date of 2220-05-08", ex.message)
    }

    /**
     * Date is [Comparable], so we should be able to tell that one date is
     * before another.
     */
    @Test
    fun testComparisonsBetweenDates() {
        assertTrue(Date(18262) < Date(18263))
        assertTrue(Date(18262) == Date(18262))
        assertTrue(Date(18263) > Date(18262))
    }
}