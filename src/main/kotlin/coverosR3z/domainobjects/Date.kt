package coverosR3z.domainobjects

import coverosR3z.misc.checkParseToInt
import kotlinx.serialization.Serializable
import java.lang.IllegalStateException
import java.time.LocalDate

private const val dateStringNotNullMsg = "The date string value must not be null"
private const val dateStringNotBlankMsg = "The date string value must not be blank"

@Serializable
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

@Serializable
class Date(val epochDay : Int) : Comparable<Date> {
    constructor(year: Int, month: Month, day: Int) : this(LocalDate.of(year, month.ord, day).toEpochDay().toInt())

    val stringValue = java.sql.Date.valueOf(LocalDate.ofEpochDay(epochDay.toLong())).toString()

    init {
        require(epochDay in 18262..47482) {"no way on earth people are using this before 2020 or past 2100, you had a date of $stringValue"}
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

        /**
         * You can pass the id as a string and we'll try to parse it
         */
        fun make(value: String?) : Date {
            val dateString = requireNotNull(value) {dateStringNotNullMsg}
            require(dateString.isNotBlank()) {dateStringNotBlankMsg}
            return checkParseToDate(dateString)
        }


        /**
         * Assumes a string similar to this: 2020-06-25
         */
        private fun checkParseToDate(value: String, msg: () -> String = {"The date string ($value) must be parsable to a date type."}) : Date {
            return try {
                val d: List<Int> = value.trim().split("-").map { checkParseToInt(it) }
                val year = d[0]
                val month = checkNotNull(Month.from(d[1]))
                val day = d[2]
                Date(year, month, day)
            } catch (ex: Throwable) {
                throw IllegalStateException(msg(), ex)
            }
        }

        fun now(): Date {
            return Date(LocalDate.now().toEpochDay().toInt())
        }

    }

}




