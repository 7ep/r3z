package coverosR3z.misc.types

import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Similar to [Date], except that we also store the time
 * of day.  We just store dates / time in GMT, and translate
 * that later.
 *
 * internal data is merely the number of seconds since the epoch - 1970-01-01
 */
class DateTime(val epochSecond : Long) : Comparable<DateTime>{
    constructor(year: Int, month: Month, day: Int, hour: Int, minute: Int, second: Int)
            : this(LocalDateTime.of(year, month.ord, day, hour, minute, second).toEpochSecond(ZoneOffset.UTC))

    val stringValue = LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC).toString()

    init {
        val beginTime = LocalDateTime.of(1980, java.time.Month.JANUARY, 1, 0, 0).toEpochSecond(ZoneOffset.UTC)
        val endTime = LocalDateTime.of(2200, java.time.Month.JANUARY, 1, 0, 0).toEpochSecond(ZoneOffset.UTC)
        require(epochSecond in beginTime..endTime) {"no way on earth people are using this before $beginTime or past $endTime, you had a date of $stringValue"}
    }

    /**
     * All we care about is the epoch second, making
     * this as simple as it can get.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DateTime

        if (epochSecond != other.epochSecond) return false

        return true
    }

    override fun toString(): String {
        return "DateTime(epochSecond=$epochSecond, $stringValue)"
    }

    override fun compareTo(other: DateTime): Int {
        return this.epochSecond.compareTo(other.epochSecond)
    }

    override fun hashCode(): Int {
        return epochSecond.hashCode()
    }

    companion object {

        // get the date right now
        fun now(): DateTime {
            return DateTime(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
        }
    }

}




