package coverosR3z.timerecording.api

import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.misc.utility.safeAttr
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.PageComponents
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.timerecording.types.*

class ViewTimeAPI(private val sd: ServerData) {

    enum class Elements (private val elemName: String = "", private val id: String = "", private val elemClass: String = "") : Element {
        PROJECT_INPUT(elemName = "project_entry"),
        CREATE_TIME_ENTRY_ROW(id="create_time_entry"),
        TIME_INPUT(elemName = "time_entry"),
        DETAIL_INPUT(elemName = "detail_entry"),
        EDIT_BUTTON(elemClass = "editbutton"),
        SAVE_BUTTON(elemClass = "savebutton"),
        DATE_INPUT(elemName = "date_entry"),
        ID_INPUT(elemName = "entry_id"),
        TIME_PERIOD(elemName = "date"),
        PREVIOUS_PERIOD(id="previous_period"),
        NEXT_PERIOD(id="next_period"),
        READ_ONLY_ROW(elemClass = "readonly-time-entry-row"),
        EDITABLE_ROW(elemClass = "editable-time-entry-row"),
        SUBMIT_BUTTON(id = "submitbutton"),
        ;
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
                "<option value =\"${it.id.value}\">${safeAttr(it.name.value)}</option>\n"
            }

        fun projectsToOptionsOneSelected(projects: List<Project>, selectedProject : Project): String {
            val sortedProjects = projects.sortedBy{it.name.value}
            return sortedProjects.joinToString("\n") {
                if (it == selectedProject) {
                    "<option selected value=\"${it.id.value}\">${safeAttr(it.name.value)}</option>"
                } else {
                    "<option value=\"${it.id.value}\">${safeAttr(it.name.value)}</option>"
                }
            }
        }

    }

    private fun existingTimeEntriesHTML() : String {
        // if we receive a query string like ?date=2020-06-12 we'll get
        // the time period it fits in
        val dateQueryString: String? = sd.ahd.queryString[Elements.TIME_PERIOD.getElemName()]
        val currentPeriod = if (dateQueryString != null) {
            val date = Date.make(dateQueryString)
            TimePeriod.getTimePeriodForDate(date)
        } else {
            TimePeriod.getTimePeriodForDate(Date.now())
        }
        val username = safeHtml(sd.ahd.user.name.value)
        val te = sd.tru.getTimeEntriesForTimePeriod(sd.ahd.user.employeeId ?: NO_EMPLOYEE.id, currentPeriod)
        val editidValue = sd.ahd.queryString["editid"]
        val projects = sd.tru.listAllProjects()
        // either get the id as an integer or get null,
        // the code will handle either properly
        val idBeingEdited = if (editidValue == null) null else checkParseToInt(editidValue)

        // Figure out time period date from viewTimeAPITests
        val periodStartDate = currentPeriod.start
        val periodEndDate = currentPeriod.end
        val inASubmittedPeriod = sd.tru.isInASubmittedPeriod(sd.ahd.user.employeeId!!, periodStartDate)
        val submitButtonLabel = if (inASubmittedPeriod) "UNSUBMIT" else "SUBMIT"
        val submitButtonAction = if (inASubmittedPeriod) UnsubmitTimeAPI.path else SubmitTimeAPI.path
        val body = """
                <h2>
                    Here are your entries, <span id="username">$username</span>
                </h2>
                <form action="$submitButtonAction" method="post">
                    <button id="${Elements.SUBMIT_BUTTON.getId()}">$submitButtonLabel</button>
                    <input name="${SubmitTimeAPI.Elements.START_DATE.getElemName()}" type="hidden" value="${periodStartDate.stringValue}">
                    <input name="${SubmitTimeAPI.Elements.END_DATE.getElemName()}" type="hidden" value="${periodEndDate.stringValue}">
                </form>
                <nav class="time_period_selector">
                    <label for="previous_period_link">prev</label>
                    <a id="${Elements.PREVIOUS_PERIOD.getId()}" href="?${Elements.TIME_PERIOD.getElemName()}=${currentPeriod.getPrevious().start.stringValue}">${currentPeriod.getPrevious().start.stringValue}</a>
                    <select>
                        <option value="UNSURE_WHAT_GOES_HERE">PREVIOUS_PERIOD</option>
                        <option selected value="current">${currentPeriod.start.stringValue} - ${currentPeriod.end.stringValue}</option>
                        <option value="UNSURE_WHAT_GOES_HERE">SUBSEQUENT_PERIOD</option>
                    </select> 
                    <label for="next_period_link">prev</label>
                    <a id="${Elements.NEXT_PERIOD.getId()}" href="?${Elements.TIME_PERIOD.getElemName()}=${currentPeriod.getNext().start.stringValue}">${currentPeriod.getNext().start.stringValue}</a>
                </nav>
                <table role="presentation">
                    <thead>
                        <tr>
                            <th class="project">Project</th>
                            <th class="date">Date</th>
                            <th class="time">Time</th>
                            <th class="details">Details</th>
                            <th class="action">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                    ${renderTimeRows(te, idBeingEdited, projects, currentPeriod)}
                    </tbody>
                </table>
        """
        return PageComponents.makeTemplate("your time entries", "ViewTimeAPI", body, extraHeaderContent="""<link rel="stylesheet" href="viewtime.css" />""" )
    }

    private fun renderTimeRows(
        te: Set<TimeEntry>,
        idBeingEdited: Int?,
        projects: List<Project>,
        currentPeriod: TimePeriod
    ): String {
        return renderCreateTimeRow(projects) +
        te.sortedBy { it.id.value }.joinToString("") {
            if (it.id.value == idBeingEdited) {
                renderEditRow(it, projects, currentPeriod)
            } else {
                renderReadOnlyRow(it, currentPeriod)
            }
        }
    }

    private fun renderReadOnlyRow(it: TimeEntry, currentPeriod: TimePeriod): String {
        return """
     <tr class="${Elements.READ_ONLY_ROW.getElemClass()}" id="time-entry-${it.id.value}">
        <div>
            <td class="project">
                <input readonly name="${Elements.PROJECT_INPUT.getElemName()}" type="text" value="${safeAttr(it.project.name.value)}" />
            </td>
            <td class="date">
                <input readonly name="${Elements.DATE_INPUT.getElemName()}" type="text" value="${safeAttr(it.date.stringValue)}" />
            </td>
            <td class="time">
                <input readonly name="${Elements.TIME_INPUT.getElemName()}" type="number" inputmode="decimal" step="0.25"  min="0" max="24" value="${it.time.getHoursAsString()}" />
            </td>
            <td class="details">
                <input readonly name="${Elements.DETAIL_INPUT.getElemName()}" type="text" maxlength="$MAX_DETAILS_LENGTH" value="${safeAttr(it.details.value)}"/>
            </td>
            
            <td class="action">
                <form action="$path">
                    <input type="hidden" name="editid" value="${it.id.value}" /> 
                    <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" /> 
                    <button class="${Elements.EDIT_BUTTON.getElemClass()}">edit</button>
                </form>
            </td>
            
        </div>
    </tr>
    """
    }

    private fun renderEditRow(it: TimeEntry, projects: List<Project>, currentPeriod: TimePeriod): String {
        return """
    <tr class="${Elements.EDITABLE_ROW.getElemClass()}" id="time-entry-${it.id.value}">
        <form action="$path" method="post">
            <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${it.id.value}" />
            <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
            <td class="project">
                <select name="${Elements.PROJECT_INPUT.getElemName()}" id="${Elements.PROJECT_INPUT.getId()}" />
                    ${projectsToOptionsOneSelected(projects, it.project)}
                </select>
            </td>
            <td class="date">
                <input name="${Elements.DATE_INPUT.getElemName()}" type="date" value="${safeAttr(it.date.stringValue)}" />
            </td>
            <td class="time">
                <input name="${Elements.TIME_INPUT.getElemName()}" type="number" inputmode="decimal" step="0.25"  min="0" max="24" value="${it.time.getHoursAsString()}" />
            </td>
            <td class="details">
                <input name="${Elements.DETAIL_INPUT.getElemName()}" type="text" maxlength="$MAX_DETAILS_LENGTH" value="${safeAttr(it.details.value)}"/>
            </td>
            <td class="action">
                <button class="${Elements.SAVE_BUTTON.getElemClass()}">save</button>
            </td>
        </form>
    </tr>
      """
    }

    private fun renderCreateTimeRow(projects: List<Project>) = """
        <tr id="${Elements.CREATE_TIME_ENTRY_ROW.getId()}">
            <form action="${EnterTimeAPI.path}" method="post">
                <td class="project">
                    <select name="project_entry" id="project_entry" required  />
                        <option selected disabled hidden value="">Choose a project</option>
                        ${projectsToOptions(projects)}
                    </select>
                </td>
                <td class="date" >
                    <input name="${Elements.DATE_INPUT.getElemName()}" type="date" value="${Date.now().stringValue}" required />
                </td>
                <td class="time">
                    <input name="${Elements.TIME_INPUT.getElemName()}" type="number" inputmode="decimal" step="0.25" min="0" max="24" required />
                </td>
                <td class="details">
                    <input name="${Elements.DETAIL_INPUT.getElemName()}" type="text" maxlength="$MAX_DETAILS_LENGTH"/>
                </td>
                <td class="action">
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

        val currentPeriod = data.mapping[Elements.TIME_PERIOD.getElemName()]
        val viewEntriesPage = path + if (currentPeriod.isNullOrBlank()) "" else "" + "?" + Elements.TIME_PERIOD.getElemName() + "=" + currentPeriod
        return redirectTo(viewEntriesPage)
    }

}