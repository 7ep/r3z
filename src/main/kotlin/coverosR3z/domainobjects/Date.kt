package coverosR3z.domainobjects

import java.time.LocalDate
import java.util.*


enum class Month(val ord: Int) {
    JAN(1), FEB(2), MAR(3), APR(4), MAY(5), JUN(6),
    JUL(7), AUG(8), SEP(9), OCT(10), NOV(11), DEC(12);

    companion object {
        fun from(ord: Int): Month? = values().find { it.ord == ord }
    }
}

/**
 * A simplistic representation of date - simply and solely holds information
 * about a year, a month, and a day of the month.  Unlike complicated classes
 * like [java.sql.Date] or [java.util.Date] or [java.util.GregorianCalendar],
 * all this has is those three values.  In contrast, those other classes
 * have tricky little bits inside to represent a more complex world.
 */
class Date(year: Int, month: Month, day: Int) {

    // The core data - the number of days since the Epoch - day 0 is 1970-01-01
    private val epochDay = LocalDate.of(year, month.ord, day).toEpochDay()

    private val sqlDate: java.sql.Date = java.sql.Date.valueOf(LocalDate.ofEpochDay(epochDay))
    private val stringValue: String = sqlDate.toString()

    init {
        assert(year in 2020..2100) {"no way on earth people are using this before 2020 or past 2100, you had $year"}
        assert(day in 1..31) {"months only go between 1 and 31 days"}
    }

    companion object {

        /**
         * Given a date in [java.sql.Date] form, convert it to our simplified
         * date, which requires extracting the year, month, and date.  We use
         * [GregorianCalendar] for that job.
         */
        fun convertSqlDateToOurDate(date : java.sql.Date) : Date {
            val localDate = date.toLocalDate()
            return Date(localDate.year, Month.from(localDate.monthValue)!!, localDate.dayOfMonth)
        }

        fun makeDateFromEpoch(day : Long) : Date {
            val localDate = LocalDate.ofEpochDay(day)
            return Date(localDate.year, Month.from(localDate.monthValue)!!, localDate.dayOfMonth)
        }

    }

    /**
     * All we care about is the epoch day, making
     * this as simple as it can get.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Date

        if (epochDay != other.epochDay) return false

        return true
    }

    override fun hashCode(): Int {
        return epochDay.hashCode()
    }

    override fun toString(): String {
        return "Date(epochDay=$epochDay, $stringValue)"
    }

}




