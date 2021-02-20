package coverosR3z.timerecording.types

import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.checkParseToDouble
import coverosR3z.persistence.types.Deserializable
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializableCompanion
import coverosR3z.persistence.types.SerializationKeys
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.deserialize
import kotlin.math.roundToInt

const val MAX_DETAILS_LENGTH = 500
const val detailsNotNullMsg = "details must not be null"
const val noNegativeTimeMsg = "Doesn't make sense to have negative time. time in minutes: "
const val lessThanTimeInDayMsg = "Entries do not span multiple days, thus must be <=24 hrs. time in minutes: "
private const val minIdMsg = "Valid identifier values are 1 or above"


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

data class TimeEntryId(val value: Int) {
    init {
        require(value > 0) { minIdMsg }
    }

    companion object {
        fun make(value: String?) : TimeEntryId {
            return TimeEntryId(checkParseToInt(value))
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

    /**
     * Returns the number of minutes as a number of hours, to two decimal places
     */
    fun getHoursAsString() : String {
        val hours = numberOfMinutes / 60.0
        return  "%.2f".format(hours)
    }

    companion object {
        fun make(value: String?) : Time {
            return Time(checkParseToInt(value))
        }

        /**
         * This assumes we are receiving the input as hours
         */
        fun makeHoursToMinutes(value: String?): Time {
            val hours = checkParseToDouble(value)
            return Time((hours * 60).roundToInt())
        }
    }
}

/**
 * A data object that stores all the business-related needs for a time entry.
 * For example, if Matt worked for 2 hours on project "A", and had some details
 * like "this was for Coveros", this object would contain all that.
 */
data class TimeEntry (
    val id : TimeEntryId,
    val employee: Employee,
    val project: Project,
    val time: Time,
    val date: Date,
    val details : Details = Details()
) : IndexableSerializable() {

    fun toTimeEntryPreDatabase() : TimeEntryPreDatabase {
        return TimeEntryPreDatabase(employee, project, time, date, details)
    }

    override fun getIndex(): Int {
        return id.value
    }

    override val dataMappings: Map<SerializationKeys, String>
        get() = mapOf(
            Keys.ID to "${id.value}",
            Keys.EMPLOYEE_ID to "${employee.id.value}",
            Keys.PROJECT_ID to "${project.id.value}",
            Keys.TIME to "${time.numberOfMinutes}",
            Keys.DATE to "${date.epochDay}",
            Keys.DETAIL to details.value
        )

    class Deserializer(val employees: Set<Employee>, val projects: Set<Project>) : Deserializable<TimeEntry> {

        override fun deserialize(str: String) : TimeEntry {
            return deserialize(str, Companion) { entries ->

                try {
                    val id = checkParseToInt(entries[Keys.ID])
                    val empId = checkParseToInt(entries[Keys.EMPLOYEE_ID])
                    val projId = checkParseToInt(entries[Keys.PROJECT_ID])
                    val minutes = checkParseToInt(entries[Keys.TIME])
                    val epochDays = checkParseToInt(entries[Keys.DATE])
                    val detailText = entries[Keys.DETAIL]


                    val project = try {
                        projects.single { it.id == ProjectId(projId) }
                    } catch (ex: NoSuchElementException) {
                        throw DatabaseCorruptedException("Unable to find a project with the id of $projId while deserializing a time entry.  Project set size: ${projects.size}")
                    }

                    val employee = try {
                        employees.single { it.id == EmployeeId(empId) }
                    } catch (ex: NoSuchElementException) {
                        throw DatabaseCorruptedException("Unable to find an employee with the id of $empId while deserializing a time entry.  Employee set size: ${employees.size}")
                    }

                    TimeEntry(
                        TimeEntryId(id),
                        employee,
                        project,
                        Time(minutes),
                        Date(epochDays),
                        Details.make(detailText)
                    )

                } catch (ex : DatabaseCorruptedException) {
                    throw ex
                } catch (ex : Throwable) {
                    throw DatabaseCorruptedException("Unable to deserialize this text as time entry data: $str", ex)
                }
            }
        }
    }

    companion object : SerializableCompanion<Keys>(Keys.values()) {

        override val directoryName: String
            get() = "time_entries"
    }


    enum class Keys(private val keyString: String) : SerializationKeys {
        ID("i"),
        EMPLOYEE_ID("e"),
        PROJECT_ID("p"),
        TIME("t"),
        DATE("d"),
        DETAIL("dtl");

        /**
         * This needs to be a method and not just a value of the class
         * so that we can have it meet an interface specification, so
         * that we can use it in generic code
         */
        override fun getKey() : String {
            return keyString
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

