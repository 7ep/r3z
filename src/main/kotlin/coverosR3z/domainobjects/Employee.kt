package coverosR3z.domainobjects

import kotlinx.serialization.Serializable

private const val maxEmployeeCount = 100_000_000
private const val maxEmployeeNameSize = 30
private const val maxEmployeeNameSizeMsg = "Max size of employee name is $maxEmployeeNameSize"
private const val maxEmployeeMsg = "No way this company has more than 100 million employees"
private const val minIdMsg = "Valid identifier values are 1 or above"
private const val nameCannotBeEmptyMsg = "All employees must have a non-empty name"

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
        require(value.isNotEmpty()) {nameCannotBeEmptyMsg}
        require(value.length <= maxEmployeeNameSize) {maxEmployeeNameSizeMsg}
    }
}

@Serializable
data class EmployeeId(val value: Int) {
    init {
        require(value < maxEmployeeCount) { maxEmployeeMsg }
        require(value > 0) { minIdMsg }
    }
}

@Serializable
data class Employee(val id: EmployeeId, val name: EmployeeName)


