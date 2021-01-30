package coverosR3z.timerecording.api

import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.PageComponents
import coverosR3z.server.utility.ServerUtilities
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.server.utility.successHTML
import coverosR3z.timerecording.types.*

class ViewTimeAPI(private val sd: ServerData) {

    enum class Elements (private val elemName: String = "", private val id: String = "", private val elemClass: String = "") : Element {
        PROJECT_INPUT("project_entry", "project_entry"),
        CREATE_TIME_ENTRY_ROW(id="create_time_entry"),
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

        fun projectsToOptions(projects: List<Project>) =
            projects.sortedBy { it.name.value }.joinToString("") {
                "<option value =\"${it.id.value}\">${safeHtml(it.name.value)}</option>\n"
            }

    }

    private fun existingTimeEntriesHTML() : String {
        val username = safeHtml(sd.ahd.user.name.value)
        val te = sd.tru.getAllEntriesForEmployee(sd.ahd.user.employeeId ?: NO_EMPLOYEE.id)
        val editidValue = sd.ahd.queryString["editid"]
        val projects = sd.tru.listAllProjects()
        // either get the id as an integer or get null,
        // the code will handle either properly
        val idBeingEdited = if (editidValue == null) null else checkParseToInt(editidValue)
        val body = """
                <h2>
                    Here are your entries, <span id="username">$username</span>
                </h2>
                <div class="container">
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
                    ${renderTimeRows(te, idBeingEdited, projects)}
                    </tbody>
                </table>
                </div>
        """
        return PageComponents.makeTemplate("your time entries", "ViewTimeAPI", body, extraHeaderContent="""<link rel="stylesheet" href="viewtime.css" />""" )
    }

    private fun renderTimeRows(
        te: Set<TimeEntry>,
        idBeingEdited: Int?,
        projects: List<Project>
    ): String {
        return renderCreateTimeRow(projects) +
        te.sortedBy { it.id.value }.joinToString("") {
            if (it.id.value == idBeingEdited) {
                renderEditRow(it)
            } else {
                renderReadOnlyRow(it)
            }
        }
    }

    private fun renderReadOnlyRow(it: TimeEntry) = """
                         <tr id="time-entry-${it.employee.id.value}-${it.id.value}">
                            <div>
                                <td class="project">
                                    ${safeHtml(it.project.name.value)}
                                </td>
                                <td class="time">
                                    ${it.time.getHoursAsString()}
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

    private fun renderEditRow(it: TimeEntry) = """
                        <tr id="time-entry-${it.employee.id.value}-${it.id.value}">
                            <form action="$path" method="post">
                                <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${it.id.value}" />
                                <td class="project">
                                    <input type="hidden" name="${Elements.PROJECT_INPUT.getElemName()}" value="${it.project.id.value}" />
                                    ${safeHtml(it.project.name.value)}
                                </td>
                                <td class="time">
                                    <input name="${Elements.TIME_INPUT.getElemName()}" type="number" step="0.25" value="${it.time.getHoursAsString()}" />
                                </td>
                                <td class="details">
                                    <input name="${Elements.DETAIL_INPUT.getElemName()}" value="${safeHtml(it.details.value)}" />
                                </td>
                                <td class="date">
                                    <input name="${Elements.DATE_INPUT.getElemName()}" type="date" value="${it.date.stringValue}" />
                                </td>
                                <td>
                                    <button class="${Elements.SAVE_BUTTON.getElemClass()}">save</button>
                                </td>
                            </form>
                        </tr>
                          """

    private fun renderCreateTimeRow(projects: List<Project>) = """
                        <tr id="${Elements.CREATE_TIME_ENTRY_ROW.getId()}">
                            <form action="${EnterTimeAPI.path}" method="post">
                                <td class="project">
                                    <select name="project_entry" id="project_entry" required="required" />
                                        <option selected disabled hidden value="">Choose here</option>
                                        ${projectsToOptions(projects)}
                                    </select>
                                </td>
                                <td class="time">
                                    <input name="${Elements.TIME_INPUT.getElemName()}" type="number" step="0.25" />
                                </td>
                                <td class="details">
                                    <input name="${Elements.DETAIL_INPUT.getElemName()}" value="" />
                                </td>
                                <td class="date">
                                    <input name="${Elements.DATE_INPUT.getElemName()}" value="${Date.now().stringValue}" type="date" />
                                </td>
                                <td>
                                    <button class="${Elements.SAVE_BUTTON.getElemClass()}">create</button>
                                </td>
                            </form>
                        </tr>
                          """

    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data
        val tru = sd.tru
        val projectId = ProjectId.make(data.mapping[Elements.PROJECT_INPUT.getElemName()])
        val time = Time.makeHoursToMinutes(data.mapping[Elements.TIME_INPUT.getElemName()])
        val details = Details.make(data.mapping[Elements.DETAIL_INPUT.getElemName()])
        val date = Date.make(data.mapping[Elements.DATE_INPUT.getElemName()])
        val entryId = TimeEntryId.make(data.mapping[Elements.ID_INPUT.getElemName()])

        val project = tru.findProjectById(projectId)
        val employee = tru.findEmployeeById(checkNotNull(sd.ahd.user.employeeId){ employeeIdNotNullMsg })

        val timeEntry = TimeEntry(entryId, employee, project, time, date, details)
        tru.changeEntry(timeEntry)

        return redirectTo(path)
    }

}