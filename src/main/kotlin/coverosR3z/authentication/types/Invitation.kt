package coverosR3z.authentication.types

import coverosR3z.misc.types.DateTime
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.persistence.types.Deserializable
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializableCompanion
import coverosR3z.persistence.types.SerializationKeys
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.dbentryDeserialize
import coverosR3z.timerecording.types.*

/**
 * An invitation is sent to new employees, so when
 * they register it connects the user to the employee.
 *
 * Someone is only able to register a user if they
 * have an invitation
 *
 * The [InvitationCode] is a secret for the
 * intended recipient, so only they can register a new user to
 * the employee made for them.
 *
 */
data class Invitation(val id: InvitationId, val code: InvitationCode, val employee: Employee, val datetime: DateTime) : IndexableSerializable() {

    override fun getIndex(): Int {
        return id.value
    }

    override val dataMappings: Map<SerializationKeys, String>
        get() = mapOf(
            Keys.ID to "${id.value}",
            Keys.CODE to code.value,
            Keys.EMPLOYEE_ID to "${employee.id.value}",
            Keys.DATE to "${datetime.epochSecond}",
        )

    enum class Keys(override val keyString: String) : SerializationKeys {
        ID("id"),
        CODE("c"),
        EMPLOYEE_ID("eid"),
        DATE("d"),
    }

    class Deserializer(val employees: Set<Employee>) : Deserializable<Invitation> {

        override fun deserialize(str: String) : Invitation {
            return dbentryDeserialize(str, Companion) { entries ->
                val id = InvitationId.make(entries[Keys.ID])
                val code = InvitationCode.make(entries[Keys.CODE])
                val employee = employees.single{ it.id == EmployeeId.make(entries[Keys.EMPLOYEE_ID]) }
                val date = DateTime.make(entries[Keys.DATE])
                Invitation(id, code, employee, date)
            }
        }

    }

    companion object : SerializableCompanion<Keys>(Keys.values()) {
        override val directoryName: String
            get() = "invitations"

    }

}

data class InvitationCode(val value: String) {
    companion object {
        fun make(value: String?) : InvitationCode {
            return InvitationCode(checkNotNull(value))
        }
    }
}

data class InvitationId(val value: Int) {

    init {
        require(value < maxInvitationCount) { maxInvitattionMsg }
        require(value >= 0) { minEmployeeIdMsg }
    }

    companion object {

        private const val maxInvitationCount = 100
        private const val maxInvitattionMsg = "invitations are short-lived, no way should there be more than $maxInvitationCount"

        /**
         * You can pass the id as a string and we'll try to parse it
         */
        fun make(value: String?) : InvitationId {
            return InvitationId(checkParseToInt(value))
        }
    }
}
