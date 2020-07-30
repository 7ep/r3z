package coverosR3z.domainobjects

import kotlinx.serialization.Serializable

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

