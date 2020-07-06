package com.coveros.r3z.domainobjects

import java.time.LocalDate
import java.util.*


enum class Month(val ord: Int) {
    JAN(0), FEB(1), MAR(2), APR(3), MAY(4), JUN(5),
    JUL(6), AUG(7), SEP(8), OCT(9), NOV(10), DEC(11);

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
data class Date(val year: Int, val month: Month, val day: Int) {

    // have to add 1 to month's ordinal for use with LocalDate.
    // annoyingly, GregorianCalendar and LocalDate differ on the ordinal
    // they assign to months.  Gregorian does it starting with January at 0,
    // and LocalDate does it with January at 1.  Yuck.
    val sqlDate: java.sql.Date = java.sql.Date.valueOf(LocalDate.of(year, month.ord+1, day))

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
            val cal = GregorianCalendar()
            cal.time = date
            val month = Month.from(cal.get(Calendar.MONTH)) ?: throw Exception("somehow, inexplicably, month was null")
            val year = cal.get(Calendar.YEAR)
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            assert(dayOfMonth in 1..31)
            return Date(year, month, dayOfMonth)
        }

    }
}




