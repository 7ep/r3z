package coverosR3z.domainobjects

import coverosR3z.getResourceAsText
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTests {

    private val text = getResourceAsText("/coverosR3z/domainobjects/date_serialized1.txt")
    private val date = Date(18438L)

    @Test
    fun `can serialize Date`() {
        assertEquals(text, date.serialize())
    }

    @Test
    fun `can deserialize Date`() {
        assertEquals(date, Date.deserialize(text))
    }
}