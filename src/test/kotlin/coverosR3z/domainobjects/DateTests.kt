package coverosR3z.domainobjects

import org.junit.Assert.*
import org.junit.Test
import java.lang.IllegalArgumentException
import java.time.DateTimeException

class DateTests {

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

    /**
     * If we send badly-formed input, we should get a clear error message
     */
    @Test
    fun testDifferentDateConstructors_negativeCase_singleInteger() {
        val ex = assertThrows(IllegalStateException::class.java) {Date.make("33333")}
        assertEquals("Input to this function must split to exactly three parts", ex.message)
    }

    /**
     * If we send badly-formed input, we should get a clear error message
     */
    @Test
    fun testDifferentDateConstructors_negativeCase_nonInteger() {
        val ex = assertThrows(IllegalArgumentException::class.java) {Date.make("2020-Jan-06")}
        assertEquals("Must be able to parse Jan as integer", ex.message)
    }

    /**
     * What should happen if we send Jan 32nd?
     */
    @Test
    fun testDifferentDateConstructors_negativeCase_tooLargeDate() {
        val ex = assertThrows(DateTimeException::class.java) {Date.make("2020-01-32")}
        assertEquals("Invalid value for DayOfMonth (valid values 1 - 28/31): 32", ex.message)
    }

    /**
     * What should happen if we have whitespace around the input?
     */
    @Test
    fun testDifferentDateConstructors_negativeCase_extraWhitespace() {
        val ex = assertThrows(IllegalArgumentException::class.java) {Date.make("   2020-01-32   ")}
        assertEquals("Must be able to parse (SPACE)(SPACE)(SPACE)2020 as integer", ex.message)
    }

    /**
     * What should happen if we have whitespace inside the input?
     */
    @Test
    fun testDifferentDateConstructors_negativeCase_extraWhitespaceInside() {
        val ex = assertThrows(IllegalArgumentException::class.java) {Date.make("   2020-    01-32   ")}
        assertEquals("Must be able to parse (SPACE)(SPACE)(SPACE)2020 as integer", ex.message)
    }

    /**
     * A browser using HTML5 will send dates in a format like "2012-07-20"
     * let's make sure we can parse that.
     */
    @Test
    fun testDateParsedCorrectlyFromBrowser() {
        val dateInFrontendFormat = "2012-07-20"
        val actual = Date.make(dateInFrontendFormat)
        val expected = Date(2012, Month.JUL, 20)
        assertEquals("Date must parse properly when in expected, chrome date widget format", expected, actual)
    }

    @Test
    fun testDateStringValue() {
        val dateInFrontendFormat = "2012-07-20"
        val actual = Date.make(dateInFrontendFormat)
        assertEquals(dateInFrontendFormat, actual.stringValue)
    }

    @Test
    fun testDate_Ancient() {
        assertThrows(IllegalArgumentException::class.java) {Date(1979, Month.DEC, 31)}
    }

    @Test
    fun testDate_AncientOK() {
        assertEquals(Date(1980, Month.JAN, 1), Date(1980, Month.JAN, 1))
    }

    @Test
    fun testDate_DistantFuture() {
        assertThrows(IllegalArgumentException::class.java) {Date(2200, Month.JAN, 2)}
    }

    @Test
    fun testDate_DistantFutureOK() {
        assertEquals(Date(2200, Month.JAN, 1), Date(2200, Month.JAN, 1))
    }

}