package coverosR3z.domainobjects

import coverosR3z.misc.checkParseToInt
import kotlinx.serialization.Serializable

const val MAX_DETAILS_LENGTH = 500
const val timeNotNullMsg = "time_entry must not be null"
const val detailsNotNullMsg = "details must not be null"
const val timeNotBlankMsg = "time_entry must not be blank"
const val noNegativeTimeMsg = "Doesn't make sense to have negative time. time in minutes: "
const val lessThanTimeInDayMsg = "Entries do not span multiple days, thus must be <=24 hrs. time in minutes: "

@Serializable
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
@Serializable
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
 * Don't be alarmed, this is just a sneaky way to create far smaller text
 * files when we serialize [TimeEntry].
 *
 * Instead of all the types, we just do what we can to store the raw
 * values in a particular order, which cuts down the size by like 95%
 *
 * So basically, write before serializing we convert our list of time
 * entries to this, and right after deserializing we convert this to
 * full time entries. Win-Win!
 *
 * We do throw a lot of information away when we convert this over.  We'll
 * see if that hurts our performance.
 *
 * @param v the integer values we are converting
 * @param dtl the details, as a string
 */
@Serializable
data class TimeEntrySerializationSurrogate(val v: List<Int>, val dtl: String) {
    companion object {

        fun toSurrogate(te : TimeEntry) : TimeEntrySerializationSurrogate {
            val values: List<Int> = listOf(te.id, te.employee.id.value, te.project.id.value, te.time.numberOfMinutes, te.date.epochDay)
            return TimeEntrySerializationSurrogate(values, te.details.value)
        }

        fun fromSurrogate(te: TimeEntrySerializationSurrogate, employees: MutableSet<Employee>, projects: MutableSet<Project>) : TimeEntry {
            return TimeEntry(te.v[0], employees.single { it.id == EmployeeId(te.v[1])}, projects.single { it.id == ProjectId(te.v[2])}, Time(te.v[3]), Date(te.v[4]), Details(te.dtl))
        }
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

