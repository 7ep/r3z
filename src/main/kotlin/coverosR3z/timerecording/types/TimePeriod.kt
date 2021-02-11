package coverosR3z.timerecording.types

import coverosR3z.misc.types.Date
import coverosR3z.misc.types.Month
import coverosR3z.timerecording.exceptions.InvalidTimePeriodException
import java.time.LocalDate

data class TimePeriod(val start: Date, val end: Date) {

    /**
     * Helper function to determine whether a provided date falls within a period
     */
    fun contains(date: Date) : Boolean {
        return date in start .. end
    }

    fun getNext() : TimePeriod {
        val localDate = LocalDate.ofEpochDay(end.epochDay.toLong())
        val nextDay = localDate.plusDays(1)
        return getTimePeriodForDate(Date(nextDay.toEpochDay().toInt()))
    }

    fun getPrevious() : TimePeriod {
        val localDate = LocalDate.ofEpochDay(start.epochDay.toLong())
        val nextDay = localDate.minusDays(1)
        return getTimePeriodForDate(Date(nextDay.toEpochDay().toInt()))
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

            if (day <= 15) {
                return TimePeriod(Date(year, month, 1), Date(year, month, 15))
            } else {
                return TimePeriod(Date(year, month, 16), Date(year, month, month.calculateLength(year)))
            }
        }

        fun make(beginDate: Date, endDate: Date) : TimePeriod {
            val generatedTimePeriod : TimePeriod = getTimePeriodForDate(beginDate)
            if(generatedTimePeriod != TimePeriod(beginDate,endDate)) throw InvalidTimePeriodException()
            return generatedTimePeriod
        }
    }
}
