package coverosR3z.timerecording.types

import coverosR3z.misc.types.Date
import coverosR3z.misc.types.Month
import coverosR3z.timerecording.exceptions.InvalidTimePeriodException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TimePeriodTests {

    /**
     * There should be a function that when we run it, it
     * properly determines a [TimePeriod] for us, for today
     */
    @Test
    fun testShouldGetTodayTimePeriod() {
        val todaysDate = Date(2021, Month.FEB, 2)

        val tp = TimePeriod.getTimePeriodForDate(todaysDate)

        val expectedTimePeriod = TimePeriod(Date(2021, Month.FEB, 1), Date(2021, Month.FEB, 15))
        assertEquals(expectedTimePeriod, tp)
    }

    /**
     * As a simple start, all time period are simply first through 15th and 16th through 31st
     * yes we know this overlaps several months (Hi Feb!) but it will still work in all months.
     */
    @Test
    fun testShouldValidateTimePeriod() {
        assertThrows(InvalidTimePeriodException::class.java) {
            TimePeriod.make(Date(2021, Month.FEB, 1), Date(2021, Month.FEB, 14))
        }
    }

}