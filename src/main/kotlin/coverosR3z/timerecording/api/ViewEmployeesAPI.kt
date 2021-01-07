package coverosR3z.timerecording.api

import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.GetEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.doGETRequireAuth

class ViewEmployeesAPI(private val sd: ServerData) {

    companion object : GetEndpoint {
        override fun handleGet(sd: ServerData): PreparedResponseData {
            val ve = ViewEmployeesAPI(sd)
            return doGETRequireAuth(sd.authStatus) { ve.existingEmployeesHTML() }
        }

    }

    private fun existingEmployeesHTML() : String {
        val username = safeHtml(sd.ahd.user.name.value)
        val employees = sd.tru.listAllEmployees()

        return """
        <!DOCTYPE html>        
        <html>
            <head>
                <title>Company Employees</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                table,
                td {
                    border: 1px solid #333;
                }
        
                thead,
                tfoot {
                    background-color: #333;
                    color: #fff;
                }
        
                </style>
            </head>
            <body>
                <p>
                    Here are the employees at your company, <span id="username">${safeHtml(username)}</span>
                </p>
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
        
            </body>
        </html>
        """
    }
}