package coverosR3z.domainobjects

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

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
     * Dates created with the epoch date constructor should match Dates
     * created with the year, month, day, constructor as well as those
     * created with the make function
     */
    @Test
    fun testDifferentDateConstructors() {
        val epochDate = Date(33333)
        val ymdDate = Date(2061, Month.from(4)!!,6)
        val makeDate = Date.make("33333")
        assertEquals(makeDate, epochDate)
        assertEquals(ymdDate, epochDate)
    }
}