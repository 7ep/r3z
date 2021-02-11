package coverosR3z.timerecording.types

import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.persistence.types.Deserializable
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializableCompanion
import coverosR3z.persistence.types.SerializationKeys
import coverosR3z.persistence.utility.DatabaseDiskPersistence

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

    class Deserializer : Deserializable<SubmittedPeriod> {

        override fun deserialize(str: String) : SubmittedPeriod {
            TODO("Not Implemented")
        }
    }

    companion object : SerializableCompanion {

        override val directoryName: String
            get() = "submissions"

        override fun convertToKey(s: String): SerializationKeys {
            return Keys.values().single { it.getKey() == s }
        }

        enum class Keys(private val keyString: String) : SerializationKeys {
            ID("id"),
            EMPLOYEE_ID("employee_id"),
            START_BOUND("start_bound"),
            END_BOUND("end_bound");

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

}