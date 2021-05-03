package coverosR3z.system.misc

import coverosR3z.system.misc.utility.checkParseToDouble
import coverosR3z.system.misc.utility.checkParseToInt
import coverosR3z.system.misc.utility.checkParseToLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

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

    @Test
    fun testParseLong_NegativeCase_nonNumeric() {
        val ex = assertThrows(IllegalStateException::class.java){ checkParseToLong("abc") }
        assertEquals("""Must be able to parse "abc" as a long""", ex.message)
    }

    @Test
    fun testParseDouble_NegativeCase_nonNumeric() {
        val ex = assertThrows(IllegalStateException::class.java){ checkParseToDouble("abc") }
        assertEquals("""Must be able to parse "abc" as a double""", ex.message)
    }

}