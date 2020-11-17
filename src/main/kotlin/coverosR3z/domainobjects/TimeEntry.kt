package coverosR3z.domainobjects

import kotlinx.serialization.Serializable

const val MAX_DETAILS_LENGTH = 500

@Serializable
data class Details(val value : String = "") {
    init {
        require(value.length <= MAX_DETAILS_LENGTH) { "no reason why details for a time entry would ever need to be this big. " +
                "if you have more to say than the lord's prayer, you're probably doing it wrong." }
    }
}

/**
 * a length of time, in minutes
 */
@Serializable
data class Time(val numberOfMinutes : Int) {
    init {
        require(numberOfMinutes > 0) {"Doesn't make sense to have zero or negative time"}
        require(numberOfMinutes <= 60*24) {"Entries do not span multiple days, thus must be <=24 hrs"}
    }
}

/**
 * A data object that stores all the business-related needs for a time entry.
 * For example, if Matt worked for 2 hours on project "A", and had some details
 * like "this was for Coveros", this object would contain all that.
 */
@Serializable
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
@Serializable
data class TimeEntryPreDatabase (
        val employee: Employee,
        val project: Project,
        val time: Time,
        val date: Date,
        val details : Details = Details())

