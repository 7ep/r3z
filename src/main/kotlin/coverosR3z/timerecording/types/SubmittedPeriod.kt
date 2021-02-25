package coverosR3z.timerecording.types

import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.deserialize
import coverosR3z.persistence.types.Deserializable
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializableCompanion
import coverosR3z.persistence.types.SerializationKeys

private const val minIdMsg = "Valid identifier values are 1 or above"

data class SubmissionId(val value: Int) {
    init {
        require(value > 0) { minIdMsg }
    }

    companion object {
        fun make(value: String?) : SubmissionId {
            return SubmissionId(checkParseToInt(value))
        }
    }
}

/**
 * A submitted TimePeriod in the database
 */
data class SubmittedPeriod(val id: SubmissionId, val employeeId: EmployeeId, val bounds: TimePeriod): IndexableSerializable() {

    override fun getIndex(): Int {
        return id.value
    }

    override val dataMappings: Map<SerializationKeys, String>
        get() = mapOf(
            Keys.ID to "${id.value}",
            Keys.EMPLOYEE_ID to employeeId.value.toString(),
            Keys.START_BOUND to bounds.start.stringValue,
            Keys.END_BOUND to bounds.end.stringValue
        )

    class Deserializer(val employees: Set<Employee>) : Deserializable<SubmittedPeriod> {

        override fun deserialize(str: String): SubmittedPeriod {
            return deserialize(str, Companion) { entries ->
                try {
                    val id = SubmissionId.make(entries[Keys.ID])
                    val employeeId = EmployeeId.make(entries[Keys.EMPLOYEE_ID])
                    val bounds = TimePeriod(Date.make(entries[Keys.START_BOUND]), Date.make(entries[Keys.END_BOUND]))
                    SubmittedPeriod(id, employeeId, bounds)
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
            get() = "submissions"

    }


    enum class Keys(override val keyString: String) : SerializationKeys {
        ID("id"),
        EMPLOYEE_ID("eid"),
        START_BOUND("start"),
        END_BOUND("end");
    }

}