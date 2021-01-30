package coverosR3z.timerecording.api

import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.GetEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.PageComponents

class ViewEmployeesAPI(private val sd: ServerData) {

    companion object : GetEndpoint {
        override fun handleGet(sd: ServerData): PreparedResponseData {
            val ve = ViewEmployeesAPI(sd)
            return doGETRequireAuth(sd.authStatus) { ve.existingEmployeesHTML() }
        }

        override val path: String
            get() = "employees"

    }

    private fun existingEmployeesHTML() : String {
        val username = safeHtml(sd.ahd.user.name.value)
        val employees = sd.tru.listAllEmployees()

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
                        </tr>
                    </thead>
                    <tbody>
                    """ +employees.joinToString("") { "<tr><td>${it.id.value}</td><td>${safeHtml(it.name.value)}</td></tr>\n" } + """
                    </tbody>
                </table>
                </div>
        """
            return PageComponents.makeTemplate("view employees", "ViewEmployeesAPI", body, extraHeaderContent="""<link rel="stylesheet" href="viewemployees.css" />""")
    }
}