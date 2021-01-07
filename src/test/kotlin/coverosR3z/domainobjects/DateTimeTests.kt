package coverosR3z.domainobjects

import coverosR3z.misc.types.DateTime
import coverosR3z.misc.types.Month
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DateTimeTests {

    /**
     * Super basic.  Two identical datetimes should be equal
     */
    @Test
    fun testShouldEqual() {
        val d1 = DateTime(2020, Month.NOV, 21, 11, 2, 3)
        val d2 = DateTime(2020, Month.NOV, 21, 11, 2, 3)
        assertEquals(d1, d2)
    }

    /**
     * Super basic.  Two non-identical datetimes should not be equal
     */
    @Test
    fun testShouldNotEqual() {
        val d1 = DateTime(2020, Month.NOV, 21, 11, 2, 2)
        val d2 = DateTime(2020, Month.NOV, 21, 11, 2, 3)
        assertNotEquals(d1, d2)
    }


    /**
     * Date is [Comparable], so we should be able to tell that one date is
     * before another.
     */
    @Test
    fun testComparisonsBetweenDates() {
        Assert.assertTrue(DateTime(1577836800) < DateTime(1577836801))
        Assert.assertTrue(DateTime(1577836800) == DateTime(1577836800))
        Assert.assertTrue(DateTime(1577836801) > DateTime(1577836800))
    }
}