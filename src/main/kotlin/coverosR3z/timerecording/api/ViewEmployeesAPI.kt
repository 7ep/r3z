package coverosR3z.timerecording.api

import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.types.Invitation
import coverosR3z.authentication.types.Role
import coverosR3z.misc.utility.safeAttr
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.GetEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.PageComponents
import coverosR3z.timerecording.types.Employee

class ViewEmployeesAPI(private val sd: ServerData) {

    companion object : GetEndpoint {
        override fun handleGet(sd: ServerData): PreparedResponseData {
            val ve = ViewEmployeesAPI(sd)
            return doGETRequireAuth(sd.ahd.user, Role.ADMIN) { ve.existingEmployeesHTML() }
        }

        override val path: String
            get() = "employees"

    }

    private fun existingEmployeesHTML() : String {
        val username = safeHtml(sd.ahd.user.name.value)

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
                <h2>
                    Here are the employees at your company, <span id="username">${safeHtml(username)}</span>
                </h2>
                <div class="container">
                <table>
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
            return PageComponents(sd).makeTemplate("view employees", "ViewEmployeesAPI", body, extraHeaderContent="""<link rel="stylesheet" href="viewemployees.css" />""")
    }
}