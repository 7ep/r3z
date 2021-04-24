package coverosR3z.timerecording.types

import coverosR3z.system.misc.types.Date
import coverosR3z.system.misc.types.Month
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

    /**
     * Seems to work, but let's check several months, and leap year
     */
    @Test
    fun testNumberOfWeekdays_MultipleMonths() {
        // let's count the number of weekdays in time periods starting in January 1st, 2020 (a leap year)
        var myTimePeriod2020 = TimePeriod.getTimePeriodForDate(Date(2020, Month.JAN, 1))
        // 11 in the first period of January,
        // 12 in the second period of January,
        // 10 in the first period of Feburary, etc.
        for(i in listOf(11, 12, 10, 10, 10, 12, 11)) {
            assertEquals(i, TimePeriod.numberOfWeekdays(myTimePeriod2020))
            myTimePeriod2020 = myTimePeriod2020.getNext()
        }

        // starting in Jan 1, 2021 - not a leap year
        var myTimePeriod2021 = TimePeriod.getTimePeriodForDate(Date(2021, Month.JAN, 1))
        for(i in listOf(11, 10, 11, 9, 11, 12, 11)) {
            assertEquals(i, TimePeriod.numberOfWeekdays(myTimePeriod2021))
            myTimePeriod2021 = myTimePeriod2021.getNext()
        }
    }

}