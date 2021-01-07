package coverosR3z.timerecording.api

import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.GetEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.doGETRequireAuth
import coverosR3z.timerecording.types.NO_EMPLOYEE

class ViewTimeAPI(private val sd: ServerData) {

    companion object : GetEndpoint {

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val vt = ViewTimeAPI(sd)
            return doGETRequireAuth(sd.authStatus) { vt.existingTimeEntriesHTML() }
        }

    }

    private fun existingTimeEntriesHTML() : String {
        val username = safeHtml(sd.rd.user.name.value)
        val te = sd.tru.getAllEntriesForEmployee(sd.rd.user.employeeId ?: NO_EMPLOYEE.id)
        return """
        <!DOCTYPE html>        
        <html>
            <head>
                <title>your time entries</title>
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
                    Here are your entries, <span id="username">$username</span>
                </p>
                <table>
                    <thead>
                        <tr>
                            <th>Project</th>
                            <th>Time</th>
                            <th>Details</th>
                            <th>Date</th>
                        </tr>
                    </thead>
                    <tbody>
                        
                    """ + te.joinToString("") { "<tr id=time-entry-${it.employee.id.value}-${it.id}><td>${safeHtml(it.project.name.value)}</td><td class='time'><input type=text value=${it.time.numberOfMinutes}></input></td><td>${safeHtml(it.details.value)}</td><td>${it.date.stringValue}</td></tr>\n" } + """    
                    </tbody>
                </table>
        
            </body>
        </html>
        """
    }

}