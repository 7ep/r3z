package coverosR3z.timerecording.api

import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.types.User
import coverosR3z.server.api.MessageAPI
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.EmployeeId
import coverosR3z.timerecording.types.NO_EMPLOYEE

class RoleAPI {

    enum class Elements(private val elemName: String, private val id: String) : Element  {
        EMPLOYEE_ID("employee_id", "employee_id"),
        ROLE("role", "role");

        override fun getId(): String {
            return this.id
        }

        override fun getElemName(): String {
            return this.elemName
        }

        override fun getElemClass(): String {
            throw IllegalAccessError()
        }
    }

    companion object : PostEndpoint {

        override fun handlePost(sd: ServerData): PreparedResponseData {
            return doPOSTAuthenticated(sd, requiredInputs, path, Role.SYSTEM, Role.ADMIN) { setRole(sd) }
        }

        private fun setRole(sd: ServerData): PreparedResponseData {
            val (employee, user) = obtainEmployeeAndUser(sd)
            val role = obtainRole(sd)
            sd.bc.au.addRoleToUser(user, role)
            return MessageAPI.createCustomMessageRedirect(
                "${employee.name.value} now has a role of: ${role.toString().toLowerCase()}",
                true,
                CreateEmployeeAPI.path
            )
        }

        private fun obtainRole(sd: ServerData): Role {
            val roleString = sd.ahd.data.mapping[Elements.ROLE.getElemName()]?.trim() ?: ""
            return Role.valueOf(roleString.toUpperCase())
        }

        /**
         * Takes the employee id sent, finds its employee, then
         * finds the user for that employee.
         */
        private fun obtainEmployeeAndUser(sd: ServerData): Pair<Employee, User> {
            val employeeIdString = sd.ahd.data.mapping[Elements.EMPLOYEE_ID.getElemName()]
            val employeeId = EmployeeId.make(employeeIdString)
            val employee = sd.bc.tru.findEmployeeById(employeeId)
            check(employee != NO_EMPLOYEE) { "No employee was found with an id of ${employeeId.value}" }
            val user = sd.bc.au.getUserByEmployee(employee)
            check(user != NO_USER) { "No user associated with the employee named ${employee.name.value} and id ${employee.id.value}" }
            return Pair(employee, user)
        }

        override val requiredInputs: Set<Element> = setOf(
            Elements.EMPLOYEE_ID,
            Elements.ROLE
        )

        override val path: String = "setapprover"

    }
}