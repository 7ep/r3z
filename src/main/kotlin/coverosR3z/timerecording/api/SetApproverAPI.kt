package coverosR3z.timerecording.api

import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.types.User
import coverosR3z.server.api.MessageAPI
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.server.utility.PageComponents
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.EmployeeName
import coverosR3z.timerecording.types.NO_EMPLOYEE

class SetApproverAPI {

    enum class Elements(private val elemName: String, private val id: String) : Element  {
        EMPLOYEE_INPUT("employee_id", "employee_id");

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

    companion object : GetEndpoint, PostEndpoint {
        override fun handleGet(sd: ServerData): PreparedResponseData {
            return doGETRequireAuth(sd.ahd.user, Role.ADMIN) { renderPage(sd) }
        }

        private fun renderPage(sd: ServerData): String {
            val body = """
            <div id="outermost_container">
                <div id="inner_container">
                    <form action="$path" method="post">
                        <p>
                            <label for="${Elements.EMPLOYEE_INPUT.getElemName()}">Employees with "regular" role:</label>
                            <select name="${Elements.EMPLOYEE_INPUT.getElemName()}" id="${Elements.EMPLOYEE_INPUT.getId()}" required="required" autofocus>
                                <option disabled selected value="">Select</option>
                                ${renderEmployeeOptions(sd)}
                            </select>
                        </p>
                    
                        <p>
                            <button>Set as approver</button>
                        </p>
                        <em>note: employees show in the list only if a user has been associated with them</em>
                    
                    </form>
                </div>    
            </div>
"""
            return PageComponents(sd).makeTemplate("Set as approver", "SetApproverAPI", body,
                extraHeaderContent="""
                <link rel="stylesheet" href="setapprover.css" />
                """.trimIndent())
        }

        private fun renderEmployeeOptions(sd: ServerData): String {
            val regularEmployees = sd.bc.au.listUsersByRole(Role.REGULAR).map { it.employee }.sortedBy { it.name.value }
            return regularEmployees.joinToString("") { "<option>${it.name.value}</option>" }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            return doPOSTAuthenticated(sd, requiredInputs, path, Role.SYSTEM, Role.ADMIN) { setApprover(sd) }
        }

        private fun setApprover(sd: ServerData): PreparedResponseData {
            val (employee, user) = obtainEmployeeAndUser(sd)
            sd.bc.au.addRoleToUser(user, Role.APPROVER)
            return MessageAPI.createCustomMessageRedirect(
                "${employee.name.value} is now an approver",
                true,
                path
            )
        }

        /**
         * Takes the employee id sent, finds its employee, then
         * finds the user for that employee.
         */
        private fun obtainEmployeeAndUser(sd: ServerData): Pair<Employee, User> {
            val employeeNameString = checkNotNull(sd.ahd.data.mapping[Elements.EMPLOYEE_INPUT.getElemName()])
            val employeeName = EmployeeName.make(employeeNameString.trim())
            val employee = sd.bc.tru.findEmployeeByName(employeeName)
            check(employee != NO_EMPLOYEE) { "No employee was found with a name of ${employeeName.value}" }
            val user = sd.bc.au.getUserByEmployee(employee)
            check(user != NO_USER) { "No user associated with the employee named ${employeeName.value}" }
            return Pair(employee, user)
        }

        override val requiredInputs: Set<Element> = setOf(
            Elements.EMPLOYEE_INPUT
        )

        override val path: String = "setapprover"

    }
}