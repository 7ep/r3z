package coverosR3z.domainobjects

import coverosR3z.misc.checkParseToInt

const val MAX_DETAILS_LENGTH = 500
const val timeNotNullMsg = "time_entry must not be null"
const val detailsNotNullMsg = "details must not be null"
const val timeNotBlankMsg = "time_entry must not be blank"
const val noNegativeTimeMsg = "Doesn't make sense to have negative time. time in minutes: "
const val lessThanTimeInDayMsg = "Entries do not span multiple days, thus must be <=24 hrs. time in minutes: "

data class Details(val value : String = "") {
    init {
        require(value.length <= MAX_DETAILS_LENGTH) { "no reason why details for a time entry would ever need to be this big. " +
                "if you have more to say than the lord's prayer, you're probably doing it wrong." }
    }

    companion object {
        fun make(value : String?) : Details {
            val valueNotNull = checkNotNull(value) {detailsNotNullMsg}
            return Details(valueNotNull)
        }
    }
}

/**
 * a length of time, in minutes
 */
data class Time(val numberOfMinutes : Int) {
    init {
        require(numberOfMinutes >= 0) { noNegativeTimeMsg + numberOfMinutes }
        require(numberOfMinutes <= 60*24) { lessThanTimeInDayMsg + numberOfMinutes }
    }

    companion object {
        fun make(value: String?) : Time {
            val time = checkNotNull(value) {timeNotNullMsg}
            require(time.isNotBlank()) {timeNotBlankMsg}
            val timeInt = checkParseToInt(time)
            return Time(timeInt)
        }
    }
}

/**
 * A data object that stores all the business-related needs for a time entry.
 * For example, if Matt worked for 2 hours on project "A", and had some details
 * like "this was for Coveros", this object would contain all that.
 */
data class TimeEntry (
        val id : Int,
        val employee: Employee,
        val project: Project,
        val time: Time,
        val date: Date,
        val details : Details = Details()) {

    fun toTimeEntryPreDatabase() : TimeEntryPreDatabase {
        return TimeEntryPreDatabase(employee, project, time, date, details)
    }
}

/**
 * Same as [TimeEntry] but it has no id because we haven't
 * spoken to the database yet
 */
data class TimeEntryPreDatabase (
        val employee: Employee,
        val project: Project,
        val time: Time,
        val date: Date,
        val details : Details = Details())

