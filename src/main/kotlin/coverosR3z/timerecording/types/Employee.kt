package coverosR3z.timerecording.types

import coverosR3z.system.misc.utility.checkParseToInt
import coverosR3z.persistence.types.Deserializable
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializationKeys
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.dbentryDeserialize
import coverosR3z.persistence.types.SerializableCompanion as SerializableCompanion

private const val maxEmployeeCount = 100_000_000
const val maxEmployeeNameSize = 30
private const val maxEmployeeNameSizeMsg = "Max size of employee name is $maxEmployeeNameSize"
private const val maxEmployeeMsg = "No way this company has more than 100 million employees"
const val minEmployeeIdMsg = "Valid identifier values are 0 or above, with 0 reserved for the system user"
private const val employeeNameCannotBeEmptyMsg = "All employees must have a non-empty name"
const val employeeIdNotNullMsg = "The employee id must not be null"
const val employeeNameNotNullMsg = "The employee name must not be null"
const val employeeIdCannotBeBlank = "The employee id must not be blank"

/**
 * This is used to represent no employee - just to avoid using null for an employee
 * It's a typed null, essentially
 */
val NO_EMPLOYEE = Employee(EmployeeId(maxEmployeeCount -1), EmployeeName("THIS REPRESENTS NO EMPLOYEE"))

/**
 * Holds a employee's name before we have a whole object, like [Employee]
 */
data class EmployeeName(val value: String) {
    init {
        require(value.isNotBlank()) { employeeNameCannotBeEmptyMsg }
        require(value.length <= maxEmployeeNameSize) { maxEmployeeNameSizeMsg }
    }

    companion object {
        fun make(value: String?) : EmployeeName {
            val valueNotNull = checkNotNull(value) { employeeNameNotNullMsg }
            return EmployeeName(valueNotNull)
        }
    }
}

data class EmployeeId(val value: Int) {


    init {
        require(value < maxEmployeeCount) { maxEmployeeMsg }
        require(value >= 0) { minEmployeeIdMsg }
    }

    companion object {

        /**
         * You can pass the id as a string and we'll try to parse it
         */
        fun make(value: String?) : EmployeeId {
            return EmployeeId(checkParseToInt(value, { employeeIdNotNullMsg }, { employeeIdCannotBeBlank }, { """The employee id was not interpretable as an integer.  You sent "$value".""" }))
        }
    }
}

data class Employee(val id: EmployeeId, val name: EmployeeName) : IndexableSerializable() {

    override fun getIndex(): Int {
        return id.value
    }

    override val dataMappings: Map<SerializationKeys, String>
        get() = mapOf(
            Keys.ID to "${id.value}",
            Keys.NAME to name.value
        )

    class Deserializer : Deserializable<Employee> {

        override fun deserialize(str: String) : Employee {
            return dbentryDeserialize(str, Companion) { entries ->
                val id = checkParseToInt(entries[Keys.ID])
                Employee(EmployeeId(id), EmployeeName.make((entries[Keys.NAME])))
            }
        }

    }

    companion object : SerializableCompanion<Keys>(Keys.values()) {

        override val directoryName: String
            get() = "employees"

    }

    enum class Keys(override val keyString: String) : SerializationKeys {
        ID("id"),
        NAME("name");

    }
}


