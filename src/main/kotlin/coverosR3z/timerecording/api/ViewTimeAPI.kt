package coverosR3z.timerecording.api

import coverosR3z.authentication.types.Role
import coverosR3z.misc.types.Date
import coverosR3z.misc.types.earliestAllowableDate
import coverosR3z.misc.types.latestAllowableDate
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
        CANCEL_BUTTON(id = "cancelbutton"),
        SAVE_BUTTON(id = "savebutton"),
        CREATE_BUTTON(id = "createbutton"),
        DATE_INPUT(elemName = "date_entry"),
        ID_INPUT(elemName = "entry_id"),
        TIME_PERIOD(elemName = "date"),
        PREVIOUS_PERIOD(id="previous_period"),
        CURRENT_PERIOD(id="current_period"),
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
            return doGETRequireAuth(sd.ahd.user, Role.REGULAR, Role.APPROVER, Role.ADMIN) { vt.existingTimeEntriesHTML() }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val vt = ViewTimeAPI(sd)
            return AuthUtilities.doPOSTAuthenticated(
                sd.ahd.user,
                requiredInputs,
                sd.ahd.data,
                Role.REGULAR, Role.APPROVER, Role.ADMIN
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
        val te = sd.bc.tru.getTimeEntriesForTimePeriod(sd.ahd.user.employee, currentPeriod)
        val editidValue = sd.ahd.queryString["editid"]
        val projects = sd.bc.tru.listAllProjects()
        // either get the id as an integer or get null,
        // the code will handle either properly
        val idBeingEdited = if (editidValue == null) null else checkParseToInt(editidValue)

        // Figure out time period date from viewTimeAPITests
        val periodStartDate = currentPeriod.start
        val periodEndDate = currentPeriod.end
        val inASubmittedPeriod = sd.bc.tru.isInASubmittedPeriod(sd.ahd.user.employee, periodStartDate)
        val submitButtonLabel = if (inASubmittedPeriod) "UNSUBMIT" else "SUBMIT"
        val submitButtonAction = if (inASubmittedPeriod) UnsubmitTimeAPI.path else SubmitTimeAPI.path
        val body = """
                <nav class="time_period_selector">
                    <form action="$submitButtonAction" method="post">
                        <button id="${Elements.SUBMIT_BUTTON.getId()}">$submitButtonLabel</button>
                        <input name="${SubmitTimeAPI.Elements.START_DATE.getElemName()}" type="hidden" value="${periodStartDate.stringValue}">
                        <input name="${SubmitTimeAPI.Elements.END_DATE.getElemName()}" type="hidden" value="${periodEndDate.stringValue}">
                    </form>
                    <form action="$path">
                        <button id="${Elements.CURRENT_PERIOD.getId()}">Current</button>
                    </form>
                     <form action="$path">
                        <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.getPrevious().start.stringValue}" /> 
                        <button id="${Elements.PREVIOUS_PERIOD.getId()}">Previous</button>
                    </form>
                    <div>${currentPeriod.start.stringValue} - ${currentPeriod.end.stringValue}</div>
                    <form action="$path">
                        <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.getNext().start.stringValue}" /> 
                        <button id="${Elements.NEXT_PERIOD.getId()}">Next</button>
                    </form>
                </nav>
                ${renderMobileDataEntry(te, idBeingEdited, projects, currentPeriod, inASubmittedPeriod)}
                <div class="timerows-container">
                ${renderTimeRows(te, idBeingEdited, projects, currentPeriod, inASubmittedPeriod)}
                </div>
        """
        return PageComponents(sd).makeTemplate("your time entries", "ViewTimeAPI", body, extraHeaderContent="""<link rel="stylesheet" href="viewtime.css" />""" )
    }

    private fun renderMobileDataEntry(
        te: Set<TimeEntry>,
        idBeingEdited: Int?,
        projects: List<Project>,
        currentPeriod: TimePeriod,
        inASubmittedPeriod: Boolean): String {
        return if (! inASubmittedPeriod) {
            return if (idBeingEdited != null) {
                editTimeHTML(te.single{it.id.value == idBeingEdited}, projects, currentPeriod)
            } else {
                entertimeHTML()
            }
        } else {
            ""
        }
    }

    private fun renderTimeRows(
        te: Set<TimeEntry>,
        idBeingEdited: Int?,
        projects: List<Project>,
        currentPeriod: TimePeriod,
        inASubmittedPeriod: Boolean
    ): String {
        val timeentriesByDate = te.groupBy { it.date }
        return if (inASubmittedPeriod) {
            var resultString = ""
            for (date in timeentriesByDate.keys.sorted()) {
                resultString += "<div>${date.stringValue}</div>"
                resultString += timeentriesByDate[date]?.sortedBy { it.project.name.value }?.joinToString("") {renderReadOnlyRow(it, currentPeriod, inASubmittedPeriod)}
            }
            resultString
        } else {
            var resultString = ""
            for (date in timeentriesByDate.keys.sorted()) {
                resultString += "<div>${date.stringValue}</div>"
                resultString += timeentriesByDate[date]
                    ?.sortedBy { it.project.name.value }
                    ?.joinToString("") {
                        if (it.id.value == idBeingEdited) {
                            renderEditRow(it, projects, currentPeriod)
                        } else {
                            renderReadOnlyRow(it, currentPeriod, inASubmittedPeriod)
                        }
                    }
            }
            renderCreateTimeRow(projects) + resultString
        }

    }

    private fun renderReadOnlyRow(it: TimeEntry, currentPeriod: TimePeriod, inASubmittedPeriod: Boolean): String {

        val editButton = if (inASubmittedPeriod) "" else """
        <div class="action time-entry-information">
            <form action="$path">
                <input type="hidden" name="editid" value="${it.id.value}" /> 
                <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" /> 
                <button class="${Elements.EDIT_BUTTON.getElemClass()}">edit</button>
            </form>
        </div>"""

        return """
     <div class="${Elements.READ_ONLY_ROW.getElemClass()}" id="time-entry-${it.id.value}">
        <div class="project time-entry-information">
            <div class="readonly-data truncate" name="${Elements.PROJECT_INPUT.getElemName()}">${safeAttr(it.project.name.value)}</div>
        </div>
        <div class="date time-entry-information">
            <div class="readonly-data truncate" name="${Elements.DATE_INPUT.getElemName()}">${safeAttr(it.date.stringValue)}</div>
        </div>
        <div class="time time-entry-information">
            <div class="readonly-data truncate" name="${Elements.TIME_INPUT.getElemName()}">${it.time.getHoursAsString()}</div>
        </div>
        <div class="details time-entry-information">
            <div class="readonly-data truncate" name="${Elements.DETAIL_INPUT.getElemName()}" title="${safeAttr(it.details.value)}">${safeHtml(it.details.value)}</div>
        </div>
            $editButton
    </div>
    """
    }

    private fun renderEditRow(it: TimeEntry, projects: List<Project>, currentPeriod: TimePeriod): String {
        return """
    <div class="${Elements.EDITABLE_ROW.getElemClass()}" id="time-entry-${it.id.value}">
        <form id="edit-desktop-form" action="$path" method="post">
            <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${it.id.value}" />
            <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
            <div class="project time-entry-information">
                <select name="${Elements.PROJECT_INPUT.getElemName()}" id="${Elements.PROJECT_INPUT.getId()}" />
                    ${projectsToOptionsOneSelected(projects, it.project)}
                </select>
            </div>
            <div class="date time-entry-information">
                <input name="${Elements.DATE_INPUT.getElemName()}" type="date" min="$earliestAllowableDate" max="$latestAllowableDate" value="${safeAttr(it.date.stringValue)}" />
            </div>
            <div class="time time-entry-information">
                <input name="${Elements.TIME_INPUT.getElemName()}" type="number" inputmode="decimal" step="0.25"  min="0" max="24" value="${it.time.getHoursAsString()}" />
            </div>
            <div class="details time-entry-information">
                <input name="${Elements.DETAIL_INPUT.getElemName()}" type="text" maxlength="$MAX_DETAILS_LENGTH" value="${safeAttr(it.details.value)}"/>
            </div>
        </form>
        <form id="cancellation-form-desktop" action="$path" method="get">
            <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
        </form>
        <div id="edit-buttons-desktop" class="action time-entry-information">
            <button form="cancellation-form-desktop" id="${Elements.CANCEL_BUTTON.getId()}">Cancel</button>
            <button form="edit-desktop-form" id="${Elements.SAVE_BUTTON.getId()}">Save</button>
        </div>
    </div>
      """
    }

    private fun renderCreateTimeRow(projects: List<Project>) = """
        <div class="create-time-entry-row" id="${Elements.CREATE_TIME_ENTRY_ROW.getId()}">
            <form action="${EnterTimeAPI.path}" method="post">
                <div class="project createrow-data time-entry-information">
                    <label>Project</label>
                    <select name="project_entry" id="project_entry" required  />
                        <option selected disabled hidden value="">Choose a project</option>
                        ${projectsToOptions(projects)}
                    </select>
                </div>
                <div class="date createrow-data time-entry-information" >
                    <label>Date</label>
                    <input name="${Elements.DATE_INPUT.getElemName()}" type="date" value="${Date.now().stringValue}" min="$earliestAllowableDate" max="$latestAllowableDate" required />
                </div>
                <div class="time createrow-data time-entry-information">
                    <label>Time (hrs)</label>
                    <input name="${Elements.TIME_INPUT.getElemName()}" type="number" inputmode="decimal" step="0.25" min="0" max="24" required />
                </div>
                
                <div class="details createrow-data time-entry-information">
                    <label>Details</label>
                    <input name="${Elements.DETAIL_INPUT.getElemName()}" type="text" maxlength="$MAX_DETAILS_LENGTH"/>
                </div>
                <div class="action createrow-data time-entry-information">
                    <button id="${Elements.CREATE_BUTTON.getId()}">create</button>
                </div>
            </form>
        </div>
          """


    private fun entertimeHTML(): String {
        val projects = sd.bc.tru.listAllProjects()

        return """
            <form id="simpler_enter_time_panel" class="mobile-data-entry" action="${EnterTimeAPI.path}" method="post">
                <div class="project">
                    <label for="project_entry">Project:</label>
                    <select name="project_entry" id="project_entry" required="required" />
                        <option selected disabled hidden value="">Choose here</option>
                        ${projectsToOptions(projects)}
                
                    </select>
                </div>
    
                <div class="date">
                    <label for="${EnterTimeAPI.Elements.DATE_INPUT.getElemName()}">Date:</label>
                    <input name="${EnterTimeAPI.Elements.DATE_INPUT.getElemName()}" id="${EnterTimeAPI.Elements.DATE_INPUT.getId()}" type="date" value="${Date.now().stringValue}" />
                </div>
                
                <div class="time">
                    <label for="${EnterTimeAPI.Elements.TIME_INPUT.getElemName()}">Time:</label>
                    <input name="${EnterTimeAPI.Elements.TIME_INPUT.getElemName()}" id="${EnterTimeAPI.Elements.TIME_INPUT.getId()}" type="number" inputmode="decimal" step="0.25" min="0" max="24" required="required" />
                </div>
                
                <div class="details">
                    <label for="${EnterTimeAPI.Elements.DETAIL_INPUT.getElemName()}">Details:</label>
                    <input name="${EnterTimeAPI.Elements.DETAIL_INPUT.getElemName()}" id="${EnterTimeAPI.Elements.DETAIL_INPUT.getId()}" type="text" maxlength="$MAX_DETAILS_LENGTH" />
                </div>
                
                <div class="action">
                    <button id="${EnterTimeAPI.Elements.ENTER_TIME_BUTTON.getId()}">Enter time</button>
                </div>
    
            </form>
    """
    }

    /**
     * Similar to [entertimeHTML] but for editing entries
     */
    private fun editTimeHTML(te: TimeEntry, projects: List<Project>, currentPeriod: TimePeriod): String {

        return """
            <div class="mobile-data-entry">
                <form id="simpler_edit_time_panel" action="$path" method="post">
                    <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${te.id.value}" />
                    <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
                    <div class="project">
                        <label for="project_entry">Project:</label>
                        <select name="${Elements.PROJECT_INPUT.getElemName()}" id="project_entry" required="required" />
                            <option selected disabled hidden value="">Choose here</option>
                            ${projectsToOptionsOneSelected(projects, te.project)}
                    
                    </select>
                    </div>
                    
                    <div class="date">
                        <label for="${Elements.DATE_INPUT.getElemName()}">Date:</label>
                        <input name="${Elements.DATE_INPUT.getElemName()}" 
                            type="date" value="${te.date.stringValue}" />
                    </div>
        
                    <div class="time">
                        <label for="${Elements.TIME_INPUT.getElemName()}">Time:</label>
                        <input name="${Elements.TIME_INPUT.getElemName()}" 
                            type="number" inputmode="decimal" 
                            step="0.25" min="0" max="24" required="required"
                            value="${te.time.getHoursAsString()}" 
                             />
                    </div>
        
                    <div class="details">
                        <label for="${Elements.DETAIL_INPUT.getElemName()}">Details:</label>
                        <input name="${Elements.DETAIL_INPUT.getElemName()}" 
                            type="text" maxlength="$MAX_DETAILS_LENGTH"
                            value="${safeAttr(te.details.value)}"
                             />
                    </div>
                    
                    
                </form>
                <form id="cancellation_form" action="$path" method="get">
                    <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
                </form>
                <div id="edit-buttons-mobile" class="action">
                    <button form="cancellation_form" id="${Elements.CANCEL_BUTTON.getId()}">Cancel</button>
                    <button form="simpler_edit_time_panel" id="${Elements.SAVE_BUTTON.getId()}">Save</button>
                </div>
                   
            </div>
    """
    }

    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data
        val tru = sd.bc.tru
        val projectId = ProjectId.make(data.mapping[Elements.PROJECT_INPUT.getElemName()])
        val time = Time.makeHoursToMinutes(data.mapping[Elements.TIME_INPUT.getElemName()])
        val details = Details.make(data.mapping[Elements.DETAIL_INPUT.getElemName()])
        val date = Date.make(data.mapping[Elements.DATE_INPUT.getElemName()])
        val entryId = TimeEntryId.make(data.mapping[Elements.ID_INPUT.getElemName()])

        val project = tru.findProjectById(projectId)
        val employee = tru.findEmployeeById(checkNotNull(sd.ahd.user.employee.id){ employeeIdNotNullMsg })

        val timeEntry = TimeEntry(entryId, employee, project, time, date, details)
        tru.changeEntry(timeEntry)

        val currentPeriod = data.mapping[Elements.TIME_PERIOD.getElemName()]
        val viewEntriesPage = path + if (currentPeriod.isNullOrBlank()) "" else "" + "?" + Elements.TIME_PERIOD.getElemName() + "=" + currentPeriod
        return redirectTo(viewEntriesPage)
    }

}