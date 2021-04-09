package coverosR3z.misc

import coverosR3z.misc.utility.checkParseToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.lang.IllegalArgumentException

class GeneralUtilitiesTests {

    @Test
    fun testParseInteger_NegativeCase_null() {
        val ex = assertThrows(IllegalStateException::class.java){ checkParseToInt(null) }
        assertEquals("Integer must not be a null value", ex.message)
    }

    @Test
    fun testParseInteger_NegativeCase_emptyString() {
        val ex = assertThrows(IllegalStateException::class.java){ checkParseToInt("") }
        assertEquals("Integer must not be blank", ex.message)
    }

    @Test
    fun testParseInteger_NegativeCase_nonNumeric() {
        val ex = assertThrows(IllegalStateException::class.java){ checkParseToInt("abc") }
        assertEquals("""Must be able to parse "abc" as an integer""", ex.message)
    }

}