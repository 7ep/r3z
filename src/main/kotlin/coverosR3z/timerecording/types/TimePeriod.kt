package coverosR3z.timerecording.types

import coverosR3z.system.misc.types.Date
import coverosR3z.system.misc.types.Month
import coverosR3z.timerecording.exceptions.InvalidTimePeriodException
import java.time.LocalDate
import kotlin.math.floor

/**
 * Represents a period of time to contain a set of time entries
 */
data class TimePeriod(val start: Date, val end: Date) {

    /**
     * Helper function to determine whether a provided date falls within a period
     */
    fun contains(date: Date) : Boolean {
        return date in start .. end
    }

    fun getNext() : TimePeriod {
        val localDate = LocalDate.ofEpochDay(end.epochDay)
        val nextDay = localDate.plusDays(1)
        return getTimePeriodForDate(Date(nextDay.toEpochDay()))
    }

    fun getPrevious() : TimePeriod {
        val localDate = LocalDate.ofEpochDay(start.epochDay)
        val nextDay = localDate.minusDays(1)
        return getTimePeriodForDate(Date(nextDay.toEpochDay()))
    }

    companion object {
        /**
         * Return the one true valid time period for a given date. Assuming all months are divided into
         * two periods: 1st through 15th, and 16th through end of month.
         */
        fun getTimePeriodForDate(date: Date) : TimePeriod {
            val day = date.day()
            val month = Month.from(date.month())
            val year = date.year()

            return if (day <= 15) {
                TimePeriod(Date(year, month, 1), Date(year, month, 15))
            } else {
                TimePeriod(Date(year, month, 16), Date(year, month, month.calculateLength(year)))
            }
        }

        fun make(beginDate: Date, endDate: Date) : TimePeriod {
            val generatedTimePeriod : TimePeriod = getTimePeriodForDate(beginDate)
            if(generatedTimePeriod != TimePeriod(beginDate,endDate)) throw InvalidTimePeriodException()
            return generatedTimePeriod
        }


        /**
         * Returns the number of weekdays (Mon,Tues,Weds,Thurs,Fri) within this time period
         */
        fun numberOfWeekdays(currentPeriod: TimePeriod): Int {
            return (currentPeriod.start.epochDay .. currentPeriod.end.epochDay).count { floor((it + 4).toDouble()).rem(7).toInt() !in listOf(0,6) }
        }

    }
}
