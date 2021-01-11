package coverosR3z.timerecording.api

import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.GetEndpoint
import coverosR3z.server.types.PostEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.successHTML
import coverosR3z.timerecording.types.*

class ViewTimeAPI(private val sd: ServerData) {

    enum class Elements (val elemName: String, val id: String) {
        PROJECT_INPUT("project_entry", "project_entry"),
        TIME_INPUT("time_entry", "time_entry"),
        DETAIL_INPUT("detail_entry", "detail_entry"),
        ENTER_TIME_BUTTON("", "enter_time_button"),
        DATE_INPUT("date_entry", "date_entry"),
        ID_INPUT("entry_id", "entry_id"),
    }

    companion object : GetEndpoint, PostEndpoint {

        override val requiredInputs = setOf(
            Elements.PROJECT_INPUT.elemName,
            Elements.TIME_INPUT.elemName,
            Elements.DETAIL_INPUT.elemName,
            Elements.DATE_INPUT.elemName,
            Elements.ID_INPUT.elemName,
        )

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val vt = ViewTimeAPI(sd)
            return doGETRequireAuth(sd.authStatus) { vt.existingTimeEntriesHTML() }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val vt = ViewTimeAPI(sd)
            return AuthUtilities.doPOSTAuthenticated(
                sd.authStatus,
                requiredInputs,
                sd.ahd.data
            ) { vt.handlePOST() }
        }

        override val path: String
            get() = "timeentries"

    }

    private fun existingTimeEntriesHTML() : String {
        val username = safeHtml(sd.ahd.user.name.value)
        val te = sd.tru.getAllEntriesForEmployee(sd.ahd.user.employeeId ?: NO_EMPLOYEE.id)
        return """
        <!DOCTYPE html>        
        <html>
            <head>
                <title>your time entries</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta apifile="ViewTimeAPI" >
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
                        
                    """ + te.joinToString("") { "<tr id=time-entry-${it.employee.id.value}-${it.id.value}><td>${safeHtml(it.project.name.value)}</td><td class='time'><input type=text value=${it.time.numberOfMinutes}></input></td><td>${safeHtml(it.details.value)}</td><td>${it.date.stringValue}</td></tr>\n" } + """    
                    </tbody>
                </table>
        
            </body>
        </html>
        """
    }

    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data
        val tru = sd.tru
        val projectId = ProjectId.make(data[Elements.PROJECT_INPUT.elemName])
        val time = Time.make(data[Elements.TIME_INPUT.elemName])
        val details = Details.make(data[Elements.DETAIL_INPUT.elemName])
        val date = Date.make(data[Elements.DATE_INPUT.elemName])
        val entryId = TimeEntryId.make(data[Elements.ID_INPUT.elemName])

        val project = tru.findProjectById(projectId)
        val employee = tru.findEmployeeById(checkNotNull(sd.ahd.user.employeeId){ employeeIdNotNullMsg })

        val timeEntry = TimeEntry(entryId, employee, project, time, date, details)
        tru.changeEntry(timeEntry)

        return okHTML(successHTML)
    }

}