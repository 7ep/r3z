package coverosR3z.domainobjects

import kotlinx.serialization.Serializable

private const val maxEmployeeCount = 100_000_000
private const val maxEmployeeMsg = "No way this company has more than 100 million employees"
private const val minIdMsg = "Valid identifier values are 1 or above"
private const val nameCannotBeEmptyMsg = "All employees must have a non-empty name"


/**
 * Holds a employee's name before we have a whole object, like [Employee]
 */
@Serializable
data class EmployeeName(val value: String) {
    init {
        require(value.isNotEmpty()) {nameCannotBeEmptyMsg}
    }
}

@Serializable
data class Employee(val id: Int, val name: String) {

    init {
        require(name.isNotEmpty()) {nameCannotBeEmptyMsg}
        require(id < maxEmployeeCount) { maxEmployeeMsg }
        require(id > 0) { minIdMsg }
    }

}



