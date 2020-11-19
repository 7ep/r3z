package coverosR3z.domainobjects

import coverosR3z.misc.checkParseToInt
import kotlinx.serialization.Serializable

private const val maxEmployeeCount = 100_000_000
private const val maxEmployeeNameSize = 30
private const val maxEmployeeNameSizeMsg = "Max size of employee name is $maxEmployeeNameSize"
private const val maxEmployeeMsg = "No way this company has more than 100 million employees"
const val minEmployeeIdMsg = "Valid identifier values are 1 or above"
private const val employeeNameCannotBeEmptyMsg = "All employees must have a non-empty name"
const val employeeIdNotNullMsg = "The employee id must not be null"
const val employeeIdCannotBeBlank = "The employee id must not be blank"

/**
 * This is used to represent no employee - just to avoid using null for an employee
 * It's a typed null, essentially
 */
val NO_EMPLOYEE = Employee(EmployeeId(maxEmployeeCount-1), EmployeeName("THIS REPRESENTS NO EMPLOYEE"))

/**
 * Holds a employee's name before we have a whole object, like [Employee]
 */
@Serializable
data class EmployeeName(val value: String) {
    init {
        require(value.isNotEmpty()) {employeeNameCannotBeEmptyMsg}
        require(value.length <= maxEmployeeNameSize) {maxEmployeeNameSizeMsg}
    }
}

@Serializable
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
            val id = requireNotNull(value) {employeeIdNotNullMsg}
            require(id.isNotBlank()) {employeeIdCannotBeBlank}
            val idInt = checkParseToInt(id)
            return EmployeeId(idInt)
        }
    }
}

@Serializable
data class Employee(val id: EmployeeId, val name: EmployeeName)


