package coverosR3z.timerecording.api

import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.types.Invitation
import coverosR3z.authentication.types.Role
import coverosR3z.misc.utility.safeAttr
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.server.utility.PageComponents
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.server.utility.successHTML
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.EmployeeName

class CreateEmployeeAPI(private val sd: ServerData) {

    enum class Elements(private val elemName: String, private val id: String) : Element {
        EMPLOYEE_INPUT("employee_name", "employee_name"),
        CREATE_BUTTON("", "employee_create_button");

        override fun getId(): String {
            return this.id
        }

        override fun getElemName(): String {
            return this.elemName
        }

        override fun getElemClass(): String {
            throw NotImplementedError()
        }
    }

    companion object : GetEndpoint, PostEndpoint {

        /**
         * The required inputs for this API
         */
        override val requiredInputs = setOf(Elements.EMPLOYEE_INPUT)

        override val path: String
            get() = "createemployee"

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val ce = CreateEmployeeAPI(sd)
            return doGETRequireAuth(sd.ahd.user, Role.ADMIN) { ce.createEmployeeHTML() }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val ce = CreateEmployeeAPI(sd)
            return doPOSTAuthenticated(sd.ahd.user, requiredInputs, sd.ahd.data, Role.SYSTEM, Role.ADMIN) { ce.createEmployee() }
        }

    }

    fun createEmployee() : PreparedResponseData {
        val employeename = EmployeeName.make(sd.ahd.data.mapping[Elements.EMPLOYEE_INPUT.getElemName()])
        val employee = sd.bc.tru.createEmployee(employeename)
        sd.bc.au.createInvitation(employee)
        return redirectTo(path)
    }

    private fun existingEmployeesHTML() : String {
        // a map of employees to invitations
        val empsToInvs = mutableMapOf<Employee, Invitation?>()
        for (employee in sd.bc.tru.listAllEmployees()) {
            val invitations = sd.bc.au.listAllInvitations().filter { it.employee == employee }
            if (invitations.none()) {
                empsToInvs[employee] = null
            } else {
                for(invitation in invitations) {empsToInvs[employee] = invitation}
            }
        }

        val employeeRows =
            empsToInvs.entries.sortedBy { it.key.id.value }.joinToString("") {
                val invitationLink = if (it.value == null) "" else
                    """
        <td><a href="https://${sd.so.host}:${sd.so.sslPort}/${RegisterAPI.path}?code=${safeAttr(it.value?.code?.value ?: "")}">Invitation</a></td>""".trimIndent()
                """
<tr>
    <td>${it.key.id.value}</td>
    <td>${safeHtml(it.key.name.value)}</td>
    $invitationLink
</tr>
"""
            }

        val body = """
                <div class="container">
                <table class="employees">
                    <thead>
                        <tr>
                            <th>Identifier</th>
                            <th>Name</th>
                            <th>Invitation code</th>
                        </tr>
                    </thead>
                    <tbody>
                        $employeeRows
                    </tbody>
                </table>
                </div>
        """
        return body
    }

    private fun createEmployeeHTML() : String {
        val username = safeHtml(sd.ahd.user.name.value)

        val body = """
        <form action="$path" method="post">
        
            <p>
                Hello there, <span id="username">$username</span>!
            </p>
        
            <p>
                <label for="${Elements.EMPLOYEE_INPUT.getElemName()}">Name:</label>
                <input name="${Elements.EMPLOYEE_INPUT.getElemName()}" id="${Elements.EMPLOYEE_INPUT.getId()}" type="text" />
            </p>
        
            <p>
                <button id="${Elements.CREATE_BUTTON.getId()}">Create new employee</button>
            </p>
        
        </form>
        ${existingEmployeesHTML()}
    """
        return PageComponents(sd).makeTemplate("create employee", "CreateEmployeeAPI", body, extraHeaderContent="""<link rel="stylesheet" href="createemployee.css" />""")
    }
}
