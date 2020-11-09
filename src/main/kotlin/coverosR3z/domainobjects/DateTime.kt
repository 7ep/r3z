package coverosR3z.domainobjects

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Similar to [Date], except that we also store the time
 * of day.  We just store dates / time in GMT, and translate
 * that later.
 *
 * internal data is merely the number of seconds since the epoch - 1970-01-01
 */
@Serializable
class DateTime(private val epochSecond : Long) {
    constructor(year: Int, month: Month, day: Int, hour: Int, minute: Int, second: Int)
            : this(LocalDateTime.of(year, month.ord, day, hour, minute, second).toEpochSecond(ZoneOffset.UTC))

    private fun stringValue() : String { return LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC).toString()}

    init {
        require(epochSecond in 1577836800..4102444800) {"no way on earth people are using this before 2020 or past 2100, you had a date of ${stringValue()}"}
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

    override fun hashCode(): Int {
        return epochSecond.hashCode()
    }

    override fun toString(): String {
        return "Date(epochDay=$epochSecond, ${stringValue()})"
    }

}




