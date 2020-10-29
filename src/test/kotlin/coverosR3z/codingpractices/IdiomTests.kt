package coverosR3z.codingpractices

import org.junit.Assert.*
import org.junit.Test
import java.lang.IllegalStateException
import java.lang.NullPointerException

/**
 * A useful place to test out how our tools work
 */
class IdiomTests {


    @Test
    fun testHowNullChecksWork_BangBang() {
        val value : Int? = null
        val thrownException = assertThrows(NullPointerException::class.java) { value!! }
        assertNull(thrownException.message)

    }

    @Test
    fun testHowNullChecksWork_CheckNotNull() {
        val value : Int? = null
        val thrownException = assertThrows(IllegalStateException::class.java) { checkNotNull(value) }
        assertEquals("Required value was null.", thrownException.message)
    }

    @Test
    fun testHowNullChecksWork_CheckNotNull_WithMessage() {
        val value : Int? = null
        val thrownException = assertThrows(IllegalStateException::class.java) { checkNotNull(value) {"This really really should not be null"} }
        assertEquals("This really really should not be null", thrownException.message)
    }

}