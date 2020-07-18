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
 * A simplistic representation of date.
 *
 * internal data is merely the number of days since the epoch - 1970-01-01
 */
class Date(val epochDay : Long) {
    constructor(year: Int, month: Month, day: Int) : this(LocalDate.of(year, month.ord, day).toEpochDay())

    private val sqlDate: java.sql.Date = java.sql.Date.valueOf(LocalDate.ofEpochDay(epochDay))
    private val stringValue: String = sqlDate.toString()

    init {
        assert(sqlDate.toLocalDate().year in 2020..2100) {"no way on earth people are using this before 2020 or past 2100, you had ${sqlDate.toLocalDate().year}"}
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




