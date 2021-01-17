package coverosR3z.timerecording.api

import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.successHTML
import coverosR3z.timerecording.types.*

class ViewTimeAPI(private val sd: ServerData) {

    enum class Elements (private val elemName: String = "", private val id: String = "", private val elemClass: String = "") : Element {
        PROJECT_INPUT("project_entry", "project_entry"),
        TIME_INPUT("time_entry", "time_entry"),
        DETAIL_INPUT("detail_entry", "detail_entry"),
        EDIT_BUTTON(elemClass = "editbutton"),
        SAVE_BUTTON(elemClass = "savebutton"),
        DATE_INPUT("date_entry", "date_entry"),
        ID_INPUT("entry_id", "entry_id"),;

        override fun getId(): String {
            return this.id
        }

        override fun getElemName(): String {
            return this.elemName
        }

        override fun getElemClass(): String {
            return this.elemClass
        }
    }

    companion object : GetEndpoint, PostEndpoint {

        override val requiredInputs = setOf(
            Elements.PROJECT_INPUT,
            Elements.TIME_INPUT,
            Elements.DETAIL_INPUT,
            Elements.DATE_INPUT,
            Elements.ID_INPUT,
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
        val queryString = sd.ahd.queryString["editid"]

        // either get the id as an integer or get null,
        // the code will handle either properly
        val idBeingEdited = queryString?.toInt()
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
                        
                    """ + te.joinToString("") {
                    if (it.id.value == idBeingEdited) {
                        """
                    <tr id="time-entry-${it.employee.id.value}-${it.id.value}">
                        <form action="$path" method="post">
                            <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${it.id.value}" />
                            <td class="project">
                                <input type="hidden" name="${Elements.PROJECT_INPUT.getElemName()}" value="${it.project.id.value}" />
                                ${safeHtml(it.project.name.value)}
                            </td>
                            <td class="time">
                                <input name="${Elements.TIME_INPUT.getElemName()}" type=text value="${it.time.numberOfMinutes}" />
                            </td>
                            <td class="details">
                                <input name="${Elements.DETAIL_INPUT.getElemName()}" value="${safeHtml(it.details.value)}" />
                            </td>
                            <td class="date">
                                <input name="${Elements.DATE_INPUT.getElemName()}" value="${it.date.stringValue}" />
                            </td>
                            <td>
                                <button class="${Elements.SAVE_BUTTON.getElemClass()}">save</button>
                            </td>
                        </form>
                    </tr>
                      """
                    } else {
                      """
                     <tr id="time-entry-${it.employee.id.value}-${it.id.value}">
                        <div>
                            <td class="project">
                                ${safeHtml(it.project.name.value)}
                            </td>
                            <td class="time">
                                ${it.time.numberOfMinutes}
                            </td>
                            <td class="details">
                                ${safeHtml(it.details.value)}
                            </td>
                            <td class="date">
                                ${it.date.stringValue}
                            </td>
                            <td>
                                <a class="${Elements.EDIT_BUTTON.getElemClass()}" href="$path?editid=${it.id.value}">edit</a>
                            </td>
                        </div>
                    </tr>
                    """
        }
                    }  +  """
                    </tbody>
                </table>
        
            </body>
        </html>
        """
    }

    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data
        val tru = sd.tru
        val projectId = ProjectId.make(data.mapping[Elements.PROJECT_INPUT.getElemName()])
        val time = Time.make(data.mapping[Elements.TIME_INPUT.getElemName()])
        val details = Details.make(data.mapping[Elements.DETAIL_INPUT.getElemName()])
        val date = Date.make(data.mapping[Elements.DATE_INPUT.getElemName()])
        val entryId = TimeEntryId.make(data.mapping[Elements.ID_INPUT.getElemName()])

        val project = tru.findProjectById(projectId)
        val employee = tru.findEmployeeById(checkNotNull(sd.ahd.user.employeeId){ employeeIdNotNullMsg })

        val timeEntry = TimeEntry(entryId, employee, project, time, date, details)
        tru.changeEntry(timeEntry)

        return okHTML(successHTML)
    }

}