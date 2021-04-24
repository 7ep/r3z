package coverosR3z.timerecording.types

import coverosR3z.system.misc.types.Date
import coverosR3z.system.misc.types.NO_DATE
import coverosR3z.system.misc.utility.checkParseToInt
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.persistence.types.Deserializable
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializableCompanion
import coverosR3z.persistence.types.SerializationKeys
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.dbentryDeserialize

private const val minIdMsg = "Valid identifier values are 1 or above"

/**
 * This is used to represent nothing - just to avoid using null
 * It's a typed null, essentially
 */
val NullSubmittedPeriod = SubmittedPeriod(SubmissionId(Int.MAX_VALUE), NO_EMPLOYEE, TimePeriod(NO_DATE, NO_DATE), ApprovalStatus.UNAPPROVED)

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
data class SubmittedPeriod(val id: SubmissionId,
                           val employee: Employee,
                           val bounds: TimePeriod,
                           val approvalStatus: ApprovalStatus): IndexableSerializable() {

    override fun getIndex(): Int {
        return id.value
    }

    override val dataMappings: Map<SerializationKeys, String>
        get() = mapOf(
            Keys.ID to "${id.value}",
            Keys.EMPLOYEE_ID to employee.id.value.toString(),
            Keys.START_BOUND to bounds.start.stringValue,
            Keys.END_BOUND to bounds.end.stringValue,
            Keys.IS_APPROVED to approvalStatus.toString()
        )

    class Deserializer(val employees: Set<Employee>) : Deserializable<SubmittedPeriod> {

        override fun deserialize(str: String): SubmittedPeriod {
            return dbentryDeserialize(str, Companion) { entries ->
                try {
                    val id = SubmissionId.make(entries[Keys.ID])
                    val employee = employees.single{ it.id == EmployeeId.make(entries[Keys.EMPLOYEE_ID]) }
                    val bounds = TimePeriod(Date.make(entries[Keys.START_BOUND]), Date.make(entries[Keys.END_BOUND]))
                    val approved = ApprovalStatus.valueOf(checkNotNull(entries[Keys.IS_APPROVED]))
                    SubmittedPeriod(id, employee, bounds, approved)
                } catch (ex : DatabaseCorruptedException) {
                    throw ex
                } catch (ex : Throwable) {
                    throw DatabaseCorruptedException("Unable to deserialize this text as submission data: $str", ex)
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
        END_BOUND("end"),
        IS_APPROVED("appr"),
        ;
    }

}