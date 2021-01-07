package coverosR3z.timerecording.types

import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.misc.utility.decode
import coverosR3z.misc.utility.encode
import coverosR3z.misc.types.Serializable
import coverosR3z.persistence.utility.PureMemoryDatabase

private const val maxEmployeeCount = 100_000_000
private const val maxEmployeeNameSize = 30
private const val maxEmployeeNameSizeMsg = "Max size of employee name is $maxEmployeeNameSize"
private const val maxEmployeeMsg = "No way this company has more than 100 million employees"
const val minEmployeeIdMsg = "Valid identifier values are 1 or above"
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
        require(value.isNotEmpty()) { employeeNameCannotBeEmptyMsg }
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
        require(value > 0) { minEmployeeIdMsg }
    }

    companion object {

        /**
         * You can pass the id as a string and we'll try to parse it
         */
        fun make(value: String?) : EmployeeId {
            return EmployeeId(checkParseToInt(value, { employeeIdNotNullMsg }, { employeeIdCannotBeBlank }))
        }
    }
}

data class Employee(val id: EmployeeId, val name: EmployeeName) : Serializable {

    override fun serialize(): String {
        return """{ id: ${id.value} , name: ${encode(name.value)} }"""
    }

    companion object {
        fun deserialize(str: String) : Employee {
            return PureMemoryDatabase.deserializer(str, Employee::class.java) { groups ->
                val id = checkParseToInt(groups[1])
                Employee(EmployeeId(id), EmployeeName(decode(groups[3])))
            }
        }
    }
}


