package coverosR3z.domainobjects

import coverosR3z.exceptions.MalformedDataDuringSerializationException
import coverosR3z.getResourceAsText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.lang.AssertionError

class DateTests {

    private val text = getResourceAsText("/coverosR3z/domainobjects/date_serialized1.txt")
    private val errortext_badNumber = getResourceAsText("/coverosR3z/domainobjects/date_serialized_error_bad_number.txt")
    private val errortext_badKey = getResourceAsText("/coverosR3z/domainobjects/date_serialized_error_bad_key.txt")
    private val errortext_empty = getResourceAsText("/coverosR3z/domainobjects/date_serialized_error_empty.txt")
    private val errortext_tooFuture = getResourceAsText("/coverosR3z/domainobjects/date_serialized_error_toofuture.txt")
    private val errortext_tooLarge = getResourceAsText("/coverosR3z/domainobjects/date_serialized_error_toolarge.txt")
    private val date = Date(18438L)

    @Test
    fun `can serialize Date`() {
        assertEquals(text, date.serialize())
    }

    @Test
    fun `can deserialize Date`() {
        assertEquals(date, Date.deserialize(text))
    }

    /**
     * How is it handled when the number cannot be parsed?
     */
    @Test
    fun `should get exception when deserializing Date and see malformed data - bad number`() {
        val ex = assertThrows(MalformedDataDuringSerializationException::class.java) { Date.deserialize(errortext_badNumber) }
        assertEquals("was unable to deserialize this: ( {epochDay=18438L} )", ex.message)
    }

    /**
     * How is it handled when the key value is bad?
     */
    @Test
    fun `should get exception when deserializing Date and see malformed data - bad key`() {
        val ex = assertThrows(MalformedDataDuringSerializationException::class.java) { Date.deserialize(errortext_badKey) }
        assertEquals("was unable to deserialize this: ( {foo=18438} )", ex.message)
    }

    /**
     * How is it handled when the data is empty?
     */
    @Test
    fun `should get exception when deserializing Date and see malformed data - empty`() {
        val ex = assertThrows(MalformedDataDuringSerializationException::class.java) { Date.deserialize(errortext_empty) }
        assertEquals("was unable to deserialize this: (  )", ex.message)
    }

    /**
     * How is it handled when the number is too large? (like, past the year 2100? 200 years from now is 91438
     */
    @Test
    fun `should get exception when deserializing Date and see malformed data - too far in future`() {
        val ex = assertThrows(AssertionError::class.java) { Date.deserialize(errortext_tooFuture) }
        assertEquals("no way on earth people are using this before 2020 or past 2100, you had 2220", ex.message)
    }

    /**
     * How is it handled when the number is just frankly too large for the type?
     */
    @Test
    fun `should get exception when deserializing Date and see malformed data - too large`() {
        val ex = assertThrows(MalformedDataDuringSerializationException::class.java) { Date.deserialize(errortext_tooLarge) }
        assertEquals("was unable to deserialize this: ( {epochDay=123456789012345678901234567890} )", ex.message)
    }
}