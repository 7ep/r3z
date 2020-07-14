package coverosR3z.domainobjects

/**
 * A data object that stores all the business-related needs for a time entry.
 * For example, if Matt worked for 2 hours on project "A", and had some details
 * like "this was for Coveros", this object would contain all that.
 */
data class TimeEntry (
        val id : Int,
        val user: User,
        val project: Project,
        val time: Time,
        val date: Date,
        val details : Details = Details()) {

    /**
     * This just strips some of the data to create
     * a leaner version for the database
     */
    fun toTimeEntryForDatabase() : TimeEntryForDatabase{
        return TimeEntryForDatabase(id, UserId(user.id), ProjectId(project.id), time, date, details)
    }
}


/**
 * This is the version of [TimeEntry] that goes
 * into the database.  A bit more scant on the data,
 * to make our data more "normalized".  That is, for example,
 * in the database, we know a user's name from the
 * user table, given the id, so we just store the id
 * in this data.
 */
data class TimeEntryForDatabase (
        val id : Int,
        val userId : UserId,
        val projectId : ProjectId,
        val time : Time,
        val date : Date,
        val details : Details = Details())