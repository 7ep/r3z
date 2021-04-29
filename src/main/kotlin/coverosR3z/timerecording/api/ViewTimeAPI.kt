package coverosR3z.timerecording.api

import coverosR3z.authentication.exceptions.UnpermittedOperationException
import coverosR3z.authentication.types.Role
import coverosR3z.system.misc.types.Date
import coverosR3z.system.misc.utility.checkParseToInt
import coverosR3z.system.misc.utility.safeAttr
import coverosR3z.system.misc.utility.safeHtml
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.PageComponents
import coverosR3z.timerecording.types.*

class ViewTimeAPI(private val sd: ServerData) {

    enum class Elements (private val value: String = "") : Element {
        // edit fields
        PROJECT_INPUT_EDIT("edit-project-entry"),
        DATE_INPUT_EDIT("edit-date"),
        TIME_INPUT_EDIT("edit-time"),
        DETAILS_INPUT_EDIT("edit-details"),
        
        // create fields
        PROJECT_INPUT_CREATE("create-project-entry"),
        DATE_INPUT_CREATE("create-date"),
        TIME_INPUT_CREATE("create-time"),
        DETAILS_INPUT_CREATE("create-details"),

        // used for the name field, sent in POSTs
        PROJECT_INPUT("project_entry"),
        TIME_INPUT("time_entry"),
        DETAIL_INPUT("detail_entry"),
        DATE_INPUT("date_entry"),
        ID_INPUT("entry_id"),

        CREATE_TIME_ENTRY_FORM("enter_time_panel"),

        EDIT_BUTTON("editbutton"),

        /**
         * Used for creating a new time entry with a particular project
         * already selected
         */
        CANCEL_BUTTON("cancelbutton"),
        SAVE_BUTTON("savebutton"),
        DELETE_BUTTON("deletebutton"),
        CREATE_BUTTON("enter_time_button"),

        // query string items

        /**
         * a date which allows us to determine which time period to show
         */
        TIME_PERIOD("date"),

        /**
         * an employee id to allow choosing whose timesheet to show
         */
        REQUESTED_EMPLOYEE("emp"),

        /**
         * the id of a time entry we are editing
         */
        EDIT_ID("editid"),

        // navigation

        PREVIOUS_PERIOD("previous_period"),
        CURRENT_PERIOD("current_period"),
        NEXT_PERIOD("next_period"),

        // parts of the time entries

        READ_ONLY_ROW("readonly-time-entry-row"),
        SUBMIT_BUTTON("submitbutton"),

        /**
         * for approval of an employee's timesheet
         */
        EMPLOYEE_TO_APPROVE_INPUT("approval-employee"),
        ;
        override fun getId(): String {
            return this.value
        }

        override fun getElemName(): String {
            return this.value
        }

        override fun getElemClass(): String {
            return this.value
        }
    }

    companion object : GetEndpoint {

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val vt = ViewTimeAPI(sd)
            return doGETRequireAuth(sd.ahd.user, Role.REGULAR, Role.APPROVER, Role.ADMIN) { vt.renderTimeEntriesPage() }
        }

        override val path: String
            get() = "timeentries"

        fun projectsToOptions(projects: List<Project>): String {
            return projects.sortedBy { it.name.value }.joinToString("") {
                """<option value="${safeAttr(it.name.value)}" />"""
            }
        }

    }

    /**
     * Top-level function for rendering everything needed for the time entries page
     */
    private fun renderTimeEntriesPage() : String {
        // if we receive a query string like ?date=2020-06-12 we'll get
        // the time period it fits in
        val currentPeriod = obtainCurrentTimePeriod()

        // let's see if they are asking for a particular employee's information
        val (employee, reviewingOtherTimesheet) = determineCriteriaForWhoseTimesheet()

        val te = sd.bc.tru.getTimeEntriesForTimePeriod(employee, currentPeriod)
        val totalHours = Time(te.sumBy {it.time.numberOfMinutes}).getHoursAsString()
        val neededHours = Time(8 * 60 * TimePeriod.numberOfWeekdays(currentPeriod)).getHoursAsString()
        val editidValue = sd.ahd.queryString[Elements.EDIT_ID.getElemName()]

        // either get the id as an integer or get null,
        // the code will handle either properly
        val idBeingEdited = determineWhichTimeEntryIsBeingEdited(editidValue, reviewingOtherTimesheet)

        val projects = sd.bc.tru.listAllProjects()

        val approvalStatus = sd.bc.tru.isApproved(employee, currentPeriod.start)
        val (inASubmittedPeriod, submitButton) = processSubmitButton(employee, currentPeriod, reviewingOtherTimesheet, approvalStatus)
        val switchEmployeeUI = createEmployeeSwitch(currentPeriod)

        val approveUI = createApproveUI(reviewingOtherTimesheet, isSubmitted = inASubmittedPeriod, approvalStatus, employee, currentPeriod)
        val navMenu =
            createNavMenu(submitButton, switchEmployeeUI, approveUI, employee, reviewingOtherTimesheet, currentPeriod, totalHours, neededHours)

        val submittedString = if (inASubmittedPeriod) "submitted" else "unsubmitted"
        // show this if we are viewing someone else's timesheet
        val viewingHeader = if (! reviewingOtherTimesheet) "" else """<h2>Viewing ${safeHtml(employee.name.value)}'s <em>$submittedString</em> timesheet</h2>"""
        val timeEntryPanel = if (approvalStatus == ApprovalStatus.APPROVED) "" else renderTimeEntryPanel(
            te,
            idBeingEdited,
            projects,
            currentPeriod,
            inASubmittedPeriod,
            reviewingOtherTimesheet
        )
        val hideEditButtons = inASubmittedPeriod || reviewingOtherTimesheet || approvalStatus == ApprovalStatus.APPROVED

        val body = """
            <div id="outermost_container">
                $viewingHeader
                $navMenu
                $timeEntryPanel
                
                <div class="timerows-container">
                    ${renderTimeRows(te, currentPeriod, hideEditButtons)}
                </div>
            </div>
            <script src="viewtime.js"></script>
        """

        val viewingSelf = sd.ahd.user.employee == employee
        val title = if (viewingSelf) "Your time entries" else "Viewing ${safeHtml(employee.name.value)}'s $submittedString timesheet "
        return PageComponents(sd).makeTemplate(title, "ViewTimeAPI", body, extraHeaderContent="""<link rel="stylesheet" href="viewtime.css" />""" )
    }

    private fun createApproveUI(reviewingOtherTimesheet: Boolean, isSubmitted: Boolean, approvalStatus: ApprovalStatus, employee: Employee, timePeriod: TimePeriod): String {
        if (! reviewingOtherTimesheet) return ""
        val renderDisabled = if (! isSubmitted) "disabled" else ""
        val buttonHtml = if (approvalStatus == ApprovalStatus.UNAPPROVED) {
            """<button $renderDisabled>Approve</button>"""
        } else {
            """<button>Unapprove</button>"""
        }
        return """
            <form class="navitem" action="${ApproveApi.path}" method="post">
                <input type="hidden" name="${Elements.EMPLOYEE_TO_APPROVE_INPUT.getElemName()}" value="${employee.id.value}" />
                <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${timePeriod.start.stringValue}" />
                $buttonHtml
            </form>
        """.trimIndent()
    }

    private fun obtainCurrentTimePeriod(): TimePeriod {
        val dateQueryString: String? = sd.ahd.queryString[Elements.TIME_PERIOD.getElemName()]
        return calculateCurrentTimePeriod(dateQueryString)
    }

    /**
     * Creates the navigation menu for the timesheet entries.
     * There are several mechanisms at play here - what the admin
     * can see versus the approver versus the regular user.
     */
    private fun createNavMenu(
        submitButton: String,
        switchEmployeeUI: String,
        approveUI: String,
        employee: Employee,
        reviewingOtherTimesheet: Boolean,
        currentPeriod: TimePeriod,
        totalHours: String,
        neededHours: String,
    ): String {
        return """ 
                <nav id="control_panel">
                    $submitButton
                    $approveUI
                    $switchEmployeeUI
                    
                    <div id="current_period_selector">
                        <label id="current_period_selector_label">Current period selector</label>
                        ${currentPeriodButton(employee, reviewingOtherTimesheet)}
                        ${previousPeriodButton(currentPeriod, employee, reviewingOtherTimesheet)}
                        <div class="period_selector_item" id="timeperiod_display">
                            <div id="timeperiod_display_start">${currentPeriod.start.stringValue}</div>
                            <div id="timeperiod_display_end">${currentPeriod.end.stringValue}</div>
                        </div>
                        <div class="period_selector_item" id="total_hours">
                            <label>hours:</label>
                            <div id="total_hours_value">$totalHours</div>
                        </div>
                        <div class="period_selector_item" id="needed_hours">
                            <label>need:</label>
                            <div id="needed_hours_value">$neededHours</div>
                        </div>
                        ${nextPeriodButton(currentPeriod, employee, reviewingOtherTimesheet)}
                    </div>
                </nav>
                """.trimIndent()
    }

    /**
     * Creates the part of the UI that allows an admin or approver to switch
     * to seeing another person's timesheet
     */
    private fun createEmployeeSwitch(currentPeriod: TimePeriod): String {
        return if (sd.ahd.user.role != Role.ADMIN) "" else """                
                <form id="employee_switch_form" class="navitem" action="$path">
                    <label id="view_other_timesheet_label">View other timesheet</label>
                    <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
                    <select id="employee-selector" name="${Elements.REQUESTED_EMPLOYEE.getElemName()}">
                        <option selected disabled hidden value="">Self</option>
                        ${allEmployeesOptions()}
                    </select>
                    <button>Switch</button>
                </form>
                """
    }

    private fun allEmployeesOptions(): String {
        val employees = sd.bc.tru.listAllEmployees()
        return employees.filterNot { it == sd.ahd.user.employee }.joinToString{"""<option value="${it.id.value}">${it.name.value}</option>"""}
    }

    private fun nextPeriodButton(
        currentPeriod: TimePeriod,
        employee: Employee,
        reviewingOtherTimesheet: Boolean
    ): String {
        val employeeField = if (! reviewingOtherTimesheet) "" else
        """<input type="hidden" name="${Elements.REQUESTED_EMPLOYEE.getElemName()}" value="${employee.id.value}" />"""
        return """           
            <form class="period_selector_item" action="$path">
                <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.getNext().start.stringValue}" /> 
                $employeeField
                <button id="${Elements.NEXT_PERIOD.getId()}">❯</button>
            </form>
    """.trimIndent()
    }

    private fun previousPeriodButton(
        currentPeriod: TimePeriod,
        employee: Employee,
        reviewingOtherTimesheet: Boolean
    ): String {
        val employeeField = if (! reviewingOtherTimesheet) "" else
        """<input type="hidden" name="${Elements.REQUESTED_EMPLOYEE.getElemName()}" value="${employee.id.value}" />"""
        return """      
            <form class="period_selector_item" action="$path">
                <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.getPrevious().start.stringValue}" /> 
                $employeeField
                <button id="${Elements.PREVIOUS_PERIOD.getId()}">❮</button>
            </form>
     """.trimIndent()
    }

    private fun currentPeriodButton(employee: Employee, reviewingOtherTimesheet: Boolean): String {
        val employeeField = if (! reviewingOtherTimesheet) "" else
            """<input type="hidden" name="${Elements.REQUESTED_EMPLOYEE.getElemName()}" value="${employee.id.value}" />"""
        return """       
            <form class="period_selector_item" action="$path">
                <button id="${Elements.CURRENT_PERIOD.getId()}">Current</button>
                $employeeField
            </form>
     """.trimIndent()
    }

    /**
     * A particular set of time entries may be submitted or non-submitted.
     * This goes through some of the calculations for that.
     */
    private fun processSubmitButton(
        employee: Employee,
        currentPeriod: TimePeriod,
        reviewingOtherTimesheet: Boolean,
        approvalStatus: ApprovalStatus,
    ): Pair<Boolean, String> {
        if (approvalStatus == ApprovalStatus.APPROVED) return Pair(true, "")

        // Figure out time period date from viewTimeAPITests
        val periodStartDate = currentPeriod.start
        val inASubmittedPeriod = sd.bc.tru.isInASubmittedPeriod(employee, periodStartDate)
        val submitButtonLabel = if (inASubmittedPeriod) "UNSUBMIT" else "SUBMIT"
        val submitButton = if (reviewingOtherTimesheet) "" else """
    <form class="navitem" action="${SubmitTimeAPI.path}" method="post">
        <button id="${Elements.SUBMIT_BUTTON.getId()}">$submitButtonLabel</button>
        <input name="${SubmitTimeAPI.Elements.START_DATE.getElemName()}" type="hidden" value="${periodStartDate.stringValue}">
        <input name="${SubmitTimeAPI.Elements.UNSUBMIT.getElemName()}" type="hidden" value="$inASubmittedPeriod">
    </form>
    """.trimIndent()
        return Pair(inASubmittedPeriod, submitButton)
    }

    /**
     * We may receive an id of a time entry to edit.  This checks
     * whether we can render that.
     */
    private fun determineWhichTimeEntryIsBeingEdited(
        editidValue: String?,
        reviewingOtherTimesheet: Boolean
    ) = if (editidValue == null) {
        null
    } else {
        if (reviewingOtherTimesheet) {
            throw IllegalStateException(
                "If you are viewing someone else's timesheet, " +
                        "you aren't allowed to edit any fields.  " +
                        "The ${Elements.EDIT_ID.getElemName()} key in the query string is not allowed."
            )
        }
        checkParseToInt(editidValue)
    }

    /**
     * We may have received a particular employee's id to determine which timesheet
     * to show.  This checks that
     */
    private fun determineCriteriaForWhoseTimesheet(): Pair<Employee, Boolean> {
        val employeeQueryString: String? = sd.ahd.queryString[Elements.REQUESTED_EMPLOYEE.getElemName()]
        return if (employeeQueryString != null) {
            if (sd.ahd.user.role == Role.REGULAR) {
                throw UnpermittedOperationException("Your role does not allow viewing other employee's timesheets.  Your URL had a query string requesting to see a particular employee, using the key ${Elements.REQUESTED_EMPLOYEE.getElemName()}")
            }
            val id = EmployeeId.make(employeeQueryString)
            if (sd.ahd.user.employee.id == id) {
                throw IllegalStateException("Error: makes no sense to request your own timesheet (employee id in query string was your own)")
            }
            val employee = sd.bc.tru.findEmployeeById(id)
            if (employee == NO_EMPLOYEE) {
                throw java.lang.IllegalStateException("Error: employee id in query string (${id.value}) does not find any employee")
            }
            Pair(employee,true)
        } else {
            Pair(sd.ahd.user.employee, false)
        }
    }

    private fun calculateCurrentTimePeriod(dateQueryString: String?): TimePeriod {
        return if (dateQueryString != null) {
            val date = Date.make(dateQueryString)
            TimePeriod.getTimePeriodForDate(date)
        } else {
            TimePeriod.getTimePeriodForDate(Date.now())
        }
    }

    /**
     * Renders the panels for both time entry and editing time
     */
    private fun renderTimeEntryPanel(
        te: Set<TimeEntry>,
        idBeingEdited: Int?,
        projects: List<Project>,
        currentPeriod: TimePeriod,
        inASubmittedPeriod: Boolean,
        reviewingOtherTimesheet: Boolean
    ): String {
        return if (! (inASubmittedPeriod || reviewingOtherTimesheet)) {
            return if (idBeingEdited != null) {
                renderEditRow(te.single{it.id.value == idBeingEdited}, projects, currentPeriod)
            } else {
                renderCreateTimeRow(currentPeriod)
            }
        } else {
            ""
        }
    }

    private fun renderTimeRows(
        te: Set<TimeEntry>,
        currentPeriod: TimePeriod,
        hideEditButtons: Boolean,
    ): String {
        val timeentriesByDate = te.groupBy { it.date }

        var readOnlyRows = ""
        for (date in timeentriesByDate.keys.sortedDescending()) {
            readOnlyRows = renderDailyTimeEntryDivider(timeentriesByDate, date, readOnlyRows)
            readOnlyRows += timeentriesByDate[date]
                ?.sortedBy { it.project.name.value }
                ?.joinToString("") {
                        renderReadOnlyRow(it, currentPeriod, hideEditButtons)
                }
        }

        return readOnlyRows

    }

    /**
     * Show the date and calculate / show the number of hours recorded for that date
     */
    private fun renderDailyTimeEntryDivider(
        timeentriesByDate: Map<Date, List<TimeEntry>>,
        date: Date,
        readOnlyRows: String
    ): String {
        var readOnlyRows1 = readOnlyRows
        val dailyHours = Time(timeentriesByDate[date]?.sumBy { it.time.numberOfMinutes } ?: 0).getHoursAsString()
        readOnlyRows1 += "<div>${date.viewTimeHeaderFormat}, Daily hours: $dailyHours</div>"
        return readOnlyRows1
    }

    private fun renderReadOnlyRow(
        it: TimeEntry,
        currentPeriod: TimePeriod,
        hideEditButtons: Boolean,
    ): String {

        val actionButtons = if (hideEditButtons) "" else """
        <div class="action time-entry-information">
            <form action="$path">
                <input type="hidden" name="${safeAttr(Elements.EDIT_ID.getElemName())}" value="${it.id.value}" /> 
                <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" /> 
                <button class="${Elements.EDIT_BUTTON.getElemClass()}">edit</button>
            </form>
        </div>
        """

        val detailContent = if (it.details.value.isBlank()) "&nbsp;" else safeHtml(it.details.value)
        return """
     <div class="${Elements.READ_ONLY_ROW.getElemClass()}" id="time-entry-${it.id.value}">
        <div class="project time-entry-information">
            <div class="readonly-data">${safeHtml(it.project.name.value)}</div>
        </div>
        <div class="time time-entry-information">
            <div class="readonly-data">${it.time.getHoursAsString()}</div>
        </div>
        <div class="details time-entry-information">
            <div class="readonly-data">$detailContent</div>
        </div>
        $actionButtons
    </div>
    """
    }

    /**
     * For entering new time
     */
    private fun renderCreateTimeRow(currentPeriod: TimePeriod): String {
        val projects = sd.bc.tru.listAllProjects()

        return """
            <form id="${Elements.CREATE_TIME_ENTRY_FORM.getId()}" class="data-entry" action="${EnterTimeAPI.path}" method="post">
                <div class="row">
                    <div class="project">
                        <label for="${Elements.PROJECT_INPUT_CREATE.getId()}">Project:</label>
                        <input autofocus list="projects" type="text" placeholder="choose" id="${Elements.PROJECT_INPUT_CREATE.getId()}" name="${Elements.PROJECT_INPUT.getElemName()}" required="required" />
                        <datalist id="projects">
                            ${projectsToOptions(projects)}
                        </datalist>
                    </div>
        
                    <div class="date">
                        <label for="${Elements.DATE_INPUT_CREATE.getId()}">Date:</label>
                        <input  id="${Elements.DATE_INPUT_CREATE.getId()}" name="${Elements.DATE_INPUT.getElemName()}" type="date" value="${Date.now().stringValue}" min="${currentPeriod.start.stringValue}" max="${currentPeriod.end.stringValue}" />
                    </div>
                    
                    <div class="time">
                        <label for="${Elements.TIME_INPUT_CREATE.getId()}">Time:</label>
                        <input  id="${Elements.TIME_INPUT_CREATE.getId()}" name="${Elements.TIME_INPUT.getElemName()}" type="number" inputmode="decimal" step="0.25" min="0" max="24" required="required" />
                    </div>
                </div>
                
                <div class="row">
                    <div class="details">
                        <label   for="${Elements.DETAILS_INPUT_CREATE.getId()}">Details:</label>
                        <textarea id="${Elements.DETAILS_INPUT_CREATE.getId()}"name="${Elements.DETAIL_INPUT.getElemName()}" maxlength="$MAX_DETAILS_LENGTH" ></textarea>
                    </div>
                    
                    <div class="action">
                        <button id="${Elements.CREATE_BUTTON.getId()}">Enter time</button>
                    </div>
                </div>
            </form>
    """
    }

    /**
     * Similar to [renderCreateTimeRow] but for editing entries
     */
    private fun renderEditRow(te: TimeEntry, projects: List<Project>, currentPeriod: TimePeriod): String {

        return """
            <div class="data-entry">
                <form id="edit_time_panel" action="${EditTimeAPI.path}" method="post">
                    <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${te.id.value}" />
                    <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
                    <div class="row">
                        <div class="project">
                            <label for="${Elements.PROJECT_INPUT_EDIT.getId()}">Project:</label>
                            <input autofocus list="projects" type="text" id="${Elements.PROJECT_INPUT_EDIT.getId()}" name="${Elements.PROJECT_INPUT.getElemName()}" required="required" value="${safeHtml(te.project.name.value)}" />
                            <datalist id="projects">
                                ${projectsToOptions(projects)}
                            </datalist>
                        </div>
                        
                        <div class="date">
                            <label for="${Elements.DATE_INPUT_EDIT.getId()}">Date:</label>
                            <input id="${Elements.DATE_INPUT_EDIT.getId()}" name="${Elements.DATE_INPUT.getElemName()}" 
                                type="date" value="${te.date.stringValue}" min="${currentPeriod.start.stringValue}" max="${currentPeriod.end.stringValue}" />
                        </div>
            
                        <div class="time">
                            <label for="${Elements.TIME_INPUT_EDIT.getId()}">Time:</label>
                            <input id="${Elements.TIME_INPUT_EDIT.getId()}" name="${Elements.TIME_INPUT.getElemName()}" 
                                type="number" inputmode="decimal" 
                                step="0.25" min="0" max="24" required="required"
                                value="${te.time.getHoursAsString()}" 
                                 />
                        </div>
                    </div>
                    
                    <div class="row">
                        <div class="details">
                            <label for="${Elements.DETAILS_INPUT_EDIT.getId()}">Details:</label>
                            <textarea id="${Elements.DETAILS_INPUT_EDIT.getId()}" name="${Elements.DETAIL_INPUT.getElemName()}" 
                                maxlength="$MAX_DETAILS_LENGTH">${safeHtml(te.details.value)}</textarea>
                        </div>
                    </div>

                    
                        </form>
                        <form id="cancellation_form" action="$path" method="get">
                            <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
                        </form>
                        <form id="delete_form" action="${DeleteTimeAPI.path}" method="post">
                            <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${te.id.value}" />
                        </form>
                        <div id="edit-buttons" class="action">
                            <button form="cancellation_form" id="${Elements.CANCEL_BUTTON.getId()}">Cancel</button>
                            <button form="delete_form" id="${Elements.DELETE_BUTTON.getId()}">Delete</button>
                            <button form="edit_time_panel" id="${Elements.SAVE_BUTTON.getId()}">Save</button>
                        </div>
            </div>
    """
    }

}