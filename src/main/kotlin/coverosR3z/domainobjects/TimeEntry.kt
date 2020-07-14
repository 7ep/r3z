package coverosR3z.domainobjects

/**
 * A data object that stores all the business-related needs for a time entry.
 * For example, if Matt worked for 2 hours on project "A", and had some details
 * like "this was for Coveros", this object would contain all that.
 */
data class TimeEntry(val id : Int, val user: User, val project: Project, val time: Time, val date: Date, val details : Details = Details())