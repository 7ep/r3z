package coverosR3z.misc.types

import coverosR3z.misc.utility.checkParseToInt
import java.time.LocalDate
import java.text.SimpleDateFormat




const val dateNotNullMsg = "date must not be null"
const val dateNotBlankMsg = "date must not be blank"
enum class Month(val ord: Int) {
    JAN(1), FEB(2), MAR(3), APR(4), MAY(5), JUN(6),
    JUL(7), AUG(8), SEP(9), OCT(10), NOV(11), DEC(12);

    companion object {
        fun from(ord: Int): Month {
            check (ord in 1..12) { "Month must comply with arbitrary human divisions of time into 12 similarly sized chunks a year. You tried $ord."}
            return values().single { it.ord == ord }
        }
    }
}

/**
 * A simplistic representation of date.
 *
 * internal data is merely the number of days since the epoch - 1970-01-01
 */

class Date(val epochDay : Int) : Comparable<Date> {
    constructor(year: Int, month: Month, day: Int) : this(LocalDate.of(year, month.ord, day).toEpochDay().toInt())

    /**
     * Normal format "YYYY-MM-DD"
     */
    val stringValue = java.sql.Date.valueOf(LocalDate.ofEpochDay(epochDay.toLong())).toString()

    private val sdf: SimpleDateFormat = SimpleDateFormat("MMDDYYYY")

    /**
     * Chrome format "MMDDYYYY"
     */
    val chromeStringValue: String = sdf.format(java.sql.Date.valueOf(LocalDate.ofEpochDay(epochDay.toLong())))

    init {
        val beginDate = LocalDate.of(1980, 1, 1).toEpochDay()
        val endDate = LocalDate.of(2200, 1, 1).toEpochDay()
        require(epochDay.toLong() in beginDate..endDate) {
            "no way on earth people are using this before $beginDate or past $endDate, you had a date of $stringValue"
        }
    }

    fun month() : Int {
        return LocalDate.ofEpochDay(epochDay.toLong()).monthValue
    }

    fun year() : Int {
        return LocalDate.ofEpochDay(epochDay.toLong()).year
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

    override fun compareTo(other: Date): Int {
        return this.epochDay.compareTo(other.epochDay)
    }

    companion object {

        // get the date right now
        fun now(): Date {
            return Date(LocalDate.now().toEpochDay().toInt())
        }

        /**
         * Takes a string value in a format like: "2020-06-20"
         * and returns a date.
         */
        fun make(value: String?): Date {
            val valueNotNull = checkNotNull(value){ dateNotNullMsg }
            require(valueNotNull.isNotBlank()) { dateNotBlankMsg }
            val split = value.split("-")
            check(split.count() == 3) {"Input to this function must split to exactly three parts"}

            return Date(checkParseToInt(split[0]), Month.from(checkParseToInt(split[1])), checkParseToInt(split[2]))
        }
    }

}