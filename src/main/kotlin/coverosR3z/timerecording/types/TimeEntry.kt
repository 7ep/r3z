package coverosR3z.timerecording.types

import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.misc.utility.decode
import coverosR3z.misc.utility.encode
import coverosR3z.misc.types.Date
import coverosR3z.persistence.utility.PureMemoryDatabase

const val MAX_DETAILS_LENGTH = 500
const val detailsNotNullMsg = "details must not be null"
const val noNegativeTimeMsg = "Doesn't make sense to have negative time. time in minutes: "
const val lessThanTimeInDayMsg = "Entries do not span multiple days, thus must be <=24 hrs. time in minutes: "

data class Details(val value : String = "") {
    init {
        require(value.length <= MAX_DETAILS_LENGTH) { "no reason why details for a time entry would ever need to be this big. " +
                "if you have more to say than the lord's prayer, you're probably doing it wrong." }
    }

    companion object {
        fun make(value : String?) : Details {
            val valueNotNull = checkNotNull(value) { detailsNotNullMsg }
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
            return Time(checkParseToInt(value))
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
    val details : Details = Details()
) {

    fun toTimeEntryPreDatabase() : TimeEntryPreDatabase {
        return TimeEntryPreDatabase(employee, project, time, date, details)
    }

    fun serialize(): String {
        return """{ i: $id , p: ${project.id.value} , t: ${time.numberOfMinutes} , d: ${date.epochDay} , dtl: ${encode(details.value)} }"""
    }

    companion object {

        fun deserialize(str: String, employee: Employee, projects: Set<Project>) : TimeEntry {
            return PureMemoryDatabase.deserializer(str, TimeEntry::class.java) { groups ->

                try {
                    val id = checkParseToInt(groups[1])
                    val projId = checkParseToInt(groups[3])
                    val minutes = checkParseToInt(groups[5])
                    val epochDays = checkParseToInt(groups[7])
                    val detailText = decode(groups[9])


                    val project = try {
                        projects.single { it.id == ProjectId(projId) }
                    } catch (ex: NoSuchElementException) {
                        throw DatabaseCorruptedException("Unable to find a project with the id of ${projId}.  Project set size: ${projects.size}")
                    }
                    TimeEntry(
                        id,
                        employee,
                        project,
                        Time(minutes),
                        Date(epochDays),
                        Details(detailText)
                    )

                } catch (ex : DatabaseCorruptedException) {
                    throw ex
                } catch (ex : Throwable) {
                    throw DatabaseCorruptedException("Unable to deserialize this text as time entry data: $str", ex)
                }
            }
        }
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
    val details : Details = Details()
)

