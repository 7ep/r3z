package coverosR3z.timerecording.api

import coverosR3z.authentication.exceptions.UnpermittedOperationException
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.utility.RolesChecker
import coverosR3z.misc.types.Date
import coverosR3z.misc.types.earliestAllowableDate
import coverosR3z.misc.types.latestAllowableDate
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.misc.utility.safeAttr
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.PageComponents
import coverosR3z.timerecording.types.*

class ViewTimeAPI(private val sd: ServerData) {

    enum class Elements (private val value: String = "") : Element {
        // edit fields for mobile
        PROJECT_INPUT_EDIT_MOBILE("mobile-edit-project-entry"),
        DATE_INPUT_EDIT_MOBILE("mobile-edit-date"),
        TIME_INPUT_EDIT_MOBILE("mobile-edit-time"),
        DETAILS_INPUT_EDIT_MOBILE("mobile-edit-details"),
        
        // edit fields for desktop
        PROJECT_INPUT_EDIT_DESKTOP("desktop-edit-project-entry"),
        DATE_INPUT_EDIT_DESKTOP("desktop-edit-date"),
        TIME_INPUT_EDIT_DESKTOP("desktop-edit-time"),
        DETAILS_INPUT_EDIT_DESKTOP("desktop-edit-details"),
        
        // create fields for mobile
        PROJECT_INPUT_CREATE_MOBILE("mobile-create-project-entry"),
        DATE_INPUT_CREATE_MOBILE("mobile-create-date"),
        TIME_INPUT_CREATE_MOBILE("mobile-create-time"),
        DETAILS_INPUT_CREATE_MOBILE("mobile-create-details"),

        // create fields for desktop
        PROJECT_INPUT_CREATE_DESKTOP("desktop-create-project-entry"),
        DATE_INPUT_CREATE_DESKTOP("desktop-create-date"),
        TIME_INPUT_CREATE_DESKTOP("desktop-create-time"),
        DETAILS_INPUT_CREATE_DESKTOP("desktop-create-details"),

        // used for the name field, sent in POSTs
        PROJECT_INPUT("project_entry"),
        TIME_INPUT("time_entry"),
        DETAIL_INPUT("detail_entry"),
        DATE_INPUT("date_entry"),
        ID_INPUT("entry_id"),

        CREATE_TIME_ENTRY_ROW("create_time_entry"),
        CREATE_TIME_ENTRY_FORM_MOBILE("simpler_enter_time_panel"),

        EDIT_BUTTON("editbutton"),
        CANCEL_BUTTON_DESKTOP("cancelbutton_desktop"),
        SAVE_BUTTON_DESKTOP("savebutton_desktop"),
        DELETE_BUTTON_DESKTOP("deletebutton_desktop"),
        CANCEL_BUTTON_MOBILE("cancelbutton_mobile"),
        SAVE_BUTTON_MOBILE("savebutton_mobile"),
        DELETE_BUTTON_MOBILE("deletebutton_mobile"),
        CREATE_BUTTON("createbutton"),
        CREATE_BUTTON_MOBILE("enter_time_button"),

        // query string items

        // a date which allows us to determine which time period to show
        TIME_PERIOD("date"),

        // an employee id to allow choosing whose timesheet to show
        REQUESTED_EMPLOYEE("emp"),

        // the id of a time entry we are editing
        EDIT_ID("editid"),

        PREVIOUS_PERIOD("previous_period"),
        CURRENT_PERIOD("current_period"),
        NEXT_PERIOD("next_period"),
        READ_ONLY_ROW("readonly-time-entry-row"),
        EDITABLE_ROW("editable-time-entry-row"),
        SUBMIT_BUTTON("submitbutton"),
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
            return doGETRequireAuth(sd.ahd.user, Role.REGULAR, Role.APPROVER, Role.ADMIN) { vt.existingTimeEntriesHTML() }
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
        val currentPeriod = calculateCurrentTimePeriod(dateQueryString)

        // let's see if they are asking for a particular employee's information
        val employeeQueryString: String? = sd.ahd.queryString[Elements.REQUESTED_EMPLOYEE.getElemName()]
        val (employee, title, reviewingOtherTimesheet) = determineCriteriaForWhoseTimesheet(employeeQueryString)

        val te = sd.bc.tru.getTimeEntriesForTimePeriod(employee, currentPeriod)
        val totalHours = Time(te.sumBy {it.time.numberOfMinutes}).getHoursAsString()
        val editidValue = sd.ahd.queryString[Elements.EDIT_ID.getElemName()]

        // either get the id as an integer or get null,
        // the code will handle either properly
        val idBeingEdited = determineWhichTimeEntryIsBeingEdited(editidValue, reviewingOtherTimesheet)

        val projects = sd.bc.tru.listAllProjects()

        val (inASubmittedPeriod, submitButton) = processSubmitButton(currentPeriod, reviewingOtherTimesheet)
        val body = """
                <h2>Viewing ${safeHtml(employee.name.value)}'s timesheet</h2>
                <nav class="time_period_selector">
                    $submitButton
                    <form action="$path">
                        <button id="${Elements.CURRENT_PERIOD.getId()}">Current</button>
                    </form>
                     <form action="$path">
                        <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.getPrevious().start.stringValue}" /> 
                        <button id="${Elements.PREVIOUS_PERIOD.getId()}">Previous</button>
                    </form>
                    <div id="timeperiod_display">${currentPeriod.start.stringValue} - ${currentPeriod.end.stringValue}</div>
                    <div id="total_hours"><label>Total hours: </label><span id="total_hours_value">$totalHours</span></div>
                    <form action="$path">
                        <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.getNext().start.stringValue}" /> 
                        <button id="${Elements.NEXT_PERIOD.getId()}">Next</button>
                    </form>
                </nav>
                ${renderMobileDataEntry(te, idBeingEdited, projects, currentPeriod, inASubmittedPeriod, reviewingOtherTimesheet)}
                
                <div class="timerows-container">
                    ${renderTimeRows(te, idBeingEdited, projects, currentPeriod, inASubmittedPeriod, reviewingOtherTimesheet)}
                </div>
        """
        return PageComponents(sd).makeTemplate(title, "ViewTimeAPI", body, extraHeaderContent="""<link rel="stylesheet" href="viewtime.css" />""" )
    }

    /**
     * A particular set of time entries may be submitted or non-submitted.
     * This goes through some of the calculations for that.
     */
    private fun processSubmitButton(
        currentPeriod: TimePeriod,
        reviewingOtherTimesheet: Boolean
    ): Pair<Boolean, String> {
        // Figure out time period date from viewTimeAPITests
        val periodStartDate = currentPeriod.start
        val periodEndDate = currentPeriod.end
        val inASubmittedPeriod = sd.bc.tru.isInASubmittedPeriod(sd.ahd.user.employee, periodStartDate)
        val submitButtonLabel = if (inASubmittedPeriod) "UNSUBMIT" else "SUBMIT"
        val submitButtonAction = if (inASubmittedPeriod) UnsubmitTimeAPI.path else SubmitTimeAPI.path
        val submitButton = if (reviewingOtherTimesheet) "" else """"
    <form action="$submitButtonAction" method="post">
        <button id="${Elements.SUBMIT_BUTTON.getId()}">$submitButtonLabel</button>
        <input name="${SubmitTimeAPI.Elements.START_DATE.getElemName()}" type="hidden" value="${periodStartDate.stringValue}">
        <input name="${SubmitTimeAPI.Elements.END_DATE.getElemName()}" type="hidden" value="${periodEndDate.stringValue}">
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
    private fun determineCriteriaForWhoseTimesheet(employeeQueryString: String?) =
        if (employeeQueryString != null) {
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
            Triple(employee, "Viewing ${safeHtml(employee.name.value)}'s timesheet", true)
        } else {
            Triple(sd.ahd.user.employee, "your time entries", false)
        }

    private fun calculateCurrentTimePeriod(dateQueryString: String?): TimePeriod {
        val currentPeriod = if (dateQueryString != null) {
            val date = Date.make(dateQueryString)
            TimePeriod.getTimePeriodForDate(date)
        } else {
            TimePeriod.getTimePeriodForDate(Date.now())
        }
        return currentPeriod
    }

    private fun renderMobileDataEntry(
        te: Set<TimeEntry>,
        idBeingEdited: Int?,
        projects: List<Project>,
        currentPeriod: TimePeriod,
        inASubmittedPeriod: Boolean,
        reviewingOtherTimesheet: Boolean): String {
        return if (! (inASubmittedPeriod || reviewingOtherTimesheet)) {
            return if (idBeingEdited != null) {
                renderEditRowMobile(te.single{it.id.value == idBeingEdited}, projects, currentPeriod)
            } else {
                renderCreateTimeRowMobile()
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
        inASubmittedPeriod: Boolean,
        reviewingOtherTimesheet: Boolean
    ): String {
        val timeentriesByDate = te.groupBy { it.date }
        return if (inASubmittedPeriod || reviewingOtherTimesheet) {
            var resultString = ""
            for (date in timeentriesByDate.keys.sorted()) {
                val dailyHours = Time(timeentriesByDate[date]?.sumBy { it.time.numberOfMinutes } ?: 0).getHoursAsString()
                resultString += "<div>${date.viewTimeHeaderFormat}, Daily hours: $dailyHours</div>"
                resultString += timeentriesByDate[date]?.sortedBy { it.project.name.value }?.joinToString("") {renderReadOnlyRow(it, currentPeriod, inASubmittedPeriod)}
            }
            resultString
        } else {
            var resultString = ""
            for (date in timeentriesByDate.keys.sortedDescending()) {
                val dailyHours = Time(timeentriesByDate[date]?.sumBy { it.time.numberOfMinutes } ?: 0).getHoursAsString()
                resultString += "<div>${date.viewTimeHeaderFormat}, Daily hours: $dailyHours</div>"
                resultString += timeentriesByDate[date]
                    ?.sortedBy { it.project.name.value }
                    ?.joinToString("") {
                        if (it.id.value == idBeingEdited) {
                            renderEditRow(it, projects, currentPeriod)
                        } else {
                            renderReadOnlyRow(it, currentPeriod, inASubmittedPeriod)
                        }
                    }
                resultString += "<div></div>"
            }
            renderCreateTimeRow(projects) + resultString
        }

    }

    private fun renderReadOnlyRow(it: TimeEntry, currentPeriod: TimePeriod, inASubmittedPeriod: Boolean): String {

        val editButton = if (inASubmittedPeriod) "" else """
        <div class="action time-entry-information">
            <form action="$path">
                <input type="hidden" name="${safeAttr(Elements.EDIT_ID.getElemName())}" value="${it.id.value}" /> 
                <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" /> 
                <button class="${Elements.EDIT_BUTTON.getElemClass()}">edit</button>
            </form>
        </div>"""

        val detailContent = if (it.details.value.isBlank()) "&nbsp;" else safeHtml(it.details.value)
        return """
     <div class="${Elements.READ_ONLY_ROW.getElemClass()}" id="time-entry-${it.id.value}">
        <div class="project time-entry-information">
            <div class="readonly-data">${safeAttr(it.project.name.value)}</div>
        </div>
        <div class="date time-entry-information">
            <div class="readonly-data">${safeAttr(it.date.stringValue)}</div>
        </div>
        <div class="time time-entry-information">
            <div class="readonly-data">${it.time.getHoursAsString()}</div>
        </div>
        <div class="details time-entry-information">
            <div class="readonly-data">$detailContent</div>
        </div>
            $editButton
    </div>
    """
    }

    /**
     * The visible edit row when in desktop mode
     */
    private fun renderEditRow(te: TimeEntry, projects: List<Project>, currentPeriod: TimePeriod): String {
        return """
    <div class="${Elements.EDITABLE_ROW.getElemClass()}" id="time-entry-${te.id.value}">
        <form id="edit-desktop-form" action="${EditTimeAPI.path}" method="post">
            <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${te.id.value}" />
            <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
            <div class="project time-entry-information">
                <select id="${Elements.PROJECT_INPUT_EDIT_DESKTOP.getId()}" name="${Elements.PROJECT_INPUT.getElemName()}" >
                    ${projectsToOptionsOneSelected(projects, te.project)}
                </select>
            </div>
            <div class="date time-entry-information">
                <input id="${Elements.DATE_INPUT_EDIT_DESKTOP.getId()}" name="${Elements.DATE_INPUT.getElemName()}" type="date" min="$earliestAllowableDate" max="$latestAllowableDate" value="${safeAttr(te.date.stringValue)}" />
            </div>
            <div class="time time-entry-information">
                <input id="${Elements.TIME_INPUT_EDIT_DESKTOP.getId()}" name="${Elements.TIME_INPUT.getElemName()}" type="number" inputmode="decimal" step="0.25"  min="0" max="24" value="${te.time.getHoursAsString()}" />
            </div>
            <div class="details time-entry-information">
                <textarea id="${Elements.DETAILS_INPUT_EDIT_DESKTOP.getId()}" name="${Elements.DETAIL_INPUT.getElemName()}" maxlength="$MAX_DETAILS_LENGTH">${safeHtml(te.details.value)}</textarea>
            </div>
        </form>
        <form id="cancellation-form-desktop" action="$path" method="get">
            <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
        </form>
        <form id="delete_form_desktop" action="${DeleteTimeAPI.path}" method="post">
                <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${te.id.value}" />
        </form>
        <div id="edit-buttons-desktop" class="action time-entry-information">
            <button form="cancellation-form-desktop" id="${Elements.CANCEL_BUTTON_DESKTOP.getId()}">Cancel</button>
            <button form="delete_form_desktop" id="${Elements.DELETE_BUTTON_DESKTOP.getId()}">Delete</button>
            <button form="edit-desktop-form" id="${Elements.SAVE_BUTTON_DESKTOP.getId()}">Save</button>
        </div>
    </div>
    <script>
        document.getElementById('time-entry-${te.id.value}').scrollIntoView({
            behavior: 'smooth'
        });
    </script>
      """
    }

    /**
     * The row for creating new time entries for desktop
     */
    private fun renderCreateTimeRow(projects: List<Project>) = """
        <div class="create-time-entry-row" id="${Elements.CREATE_TIME_ENTRY_ROW.getId()}">
            <form action="${EnterTimeAPI.path}" method="post">
                <div class="project createrow-data time-entry-information">
                    <label for="${Elements.PROJECT_INPUT_CREATE_DESKTOP.getId()}">Project</label>
                    <select id="${Elements.PROJECT_INPUT_CREATE_DESKTOP.getId()}" name="${Elements.PROJECT_INPUT.getElemName()}" required >
                        <option selected disabled hidden value="">Choose a project</option>
                        ${projectsToOptions(projects)}
                    </select>
                </div>
                <div class="date createrow-data time-entry-information" >
                    <label for="${Elements.DATE_INPUT_CREATE_DESKTOP.getId()}">Date</label>
                    <input  id="${Elements.DATE_INPUT_CREATE_DESKTOP.getId()}" name="${Elements.DATE_INPUT.getElemName()}" type="date" value="${Date.now().stringValue}" min="$earliestAllowableDate" max="$latestAllowableDate" required />
                </div>
                <div class="time createrow-data time-entry-information">
                    <label for="${Elements.TIME_INPUT_CREATE_DESKTOP.getId()}">Time (hrs)</label>
                    <input  id="${Elements.TIME_INPUT_CREATE_DESKTOP.getId()}" name="${Elements.TIME_INPUT.getElemName()}" type="number" inputmode="decimal" step="0.25" min="0" max="24" required />
                </div>
                
                <div class="details createrow-data time-entry-information">
                    <label for="${Elements.DETAILS_INPUT_CREATE_DESKTOP.getId()}">Details</label>
                    <input  id="${Elements.DETAILS_INPUT_CREATE_DESKTOP.getId()}" name="${Elements.DETAIL_INPUT.getElemName()}" type="text" maxlength="$MAX_DETAILS_LENGTH"/>
                </div>
                <div class="action createrow-data time-entry-information">
                    <button id="${Elements.CREATE_BUTTON.getId()}">create</button>
                </div>
            </form>
        </div>
          """


    /**
     * For entering new time on a mobile device
     */
    private fun renderCreateTimeRowMobile(): String {
        val projects = sd.bc.tru.listAllProjects()

        return """
            <form id="${Elements.CREATE_TIME_ENTRY_FORM_MOBILE.getId()}" class="mobile-data-entry" action="${EnterTimeAPI.path}" method="post">
                <div class="row">
                    <div class="project">
                        <label for="${Elements.PROJECT_INPUT_CREATE_MOBILE.getId()}">Project:</label>
                        <select id="${Elements.PROJECT_INPUT_CREATE_MOBILE.getId()}" name="${Elements.PROJECT_INPUT.getElemName()}" required="required" >
                            <option selected disabled hidden value="">Choose</option>
                            ${projectsToOptions(projects)}
                        </select>
                    </div>
        
                    <div class="date">
                        <label for="${Elements.DATE_INPUT_CREATE_MOBILE.getId()}">Date:</label>
                        <input  id="${Elements.DATE_INPUT_CREATE_MOBILE.getId()}" name="${Elements.DATE_INPUT.getElemName()}" type="date" value="${Date.now().stringValue}" />
                    </div>
                    
                    <div class="time">
                        <label for="${Elements.TIME_INPUT_CREATE_MOBILE.getId()}">Time:</label>
                        <input  id="${Elements.TIME_INPUT_CREATE_MOBILE.getId()}" name="${Elements.TIME_INPUT.getElemName()}" type="number" inputmode="decimal" step="0.25" min="0" max="24" required="required" />
                    </div>
                </div>
                
                <div class="row">
                    <div class="details">
                        <label   for="${Elements.DETAILS_INPUT_CREATE_MOBILE.getId()}">Details:</label>
                        <textarea id="${Elements.DETAILS_INPUT_CREATE_MOBILE.getId()}"name="${Elements.DETAIL_INPUT.getElemName()}" maxlength="$MAX_DETAILS_LENGTH" ></textarea>
                    </div>
                    
                    <div class="action">
                        <button id="${Elements.CREATE_BUTTON_MOBILE.getId()}">Enter time</button>
                    </div>
                </div>
            </form>
    """
    }

    /**
     * Similar to [renderCreateTimeRowMobile] but for editing entries
     */
    private fun renderEditRowMobile(te: TimeEntry, projects: List<Project>, currentPeriod: TimePeriod): String {

        return """
            <div class="mobile-data-entry">
                <form id="simpler_edit_time_panel" action="${EditTimeAPI.path}" method="post">
                    <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${te.id.value}" />
                    <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
                    <div class="row">
                        <div class="project">
                            <label for="${Elements.PROJECT_INPUT_EDIT_MOBILE.getId()}">Project:</label>
                            <select id="${Elements.PROJECT_INPUT_EDIT_MOBILE.getId()}" name="${Elements.PROJECT_INPUT.getElemName()}" required="required" >
                                <option disabled hidden value="">Choose</option>
                                ${projectsToOptionsOneSelected(projects, te.project)}
                            </select>
                        </div>
                        
                        <div class="date">
                            <label for="${Elements.DATE_INPUT_EDIT_MOBILE.getId()}">Date:</label>
                            <input id="${Elements.DATE_INPUT_EDIT_MOBILE.getId()}" name="${Elements.DATE_INPUT.getElemName()}" 
                                type="date" value="${te.date.stringValue}" />
                        </div>
            
                        <div class="time">
                            <label for="${Elements.TIME_INPUT_EDIT_MOBILE.getId()}">Time:</label>
                            <input id="${Elements.TIME_INPUT_EDIT_MOBILE.getId()}" name="${Elements.TIME_INPUT.getElemName()}" 
                                type="number" inputmode="decimal" 
                                step="0.25" min="0" max="24" required="required"
                                value="${te.time.getHoursAsString()}" 
                                 />
                        </div>
                    </div>
                    
                    <div class="row">
                        <div class="details">
                            <label for="${Elements.DETAILS_INPUT_EDIT_MOBILE.getId()}">Details:</label>
                            <textarea id="${Elements.DETAILS_INPUT_EDIT_MOBILE.getId()}" name="${Elements.DETAIL_INPUT.getElemName()}" 
                                maxlength="$MAX_DETAILS_LENGTH">${safeHtml(te.details.value)}</textarea>
                        </div>
                    </div>

                    
                        </form>
                        <form id="cancellation_form" action="$path" method="get">
                            <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
                        </form>
                        <form id="delete_form_mobile" action="${DeleteTimeAPI.path}" method="post">
                            <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${te.id.value}" />
                        </form>
                        <div id="edit-buttons-mobile" class="action">
                            <button form="cancellation_form" id="${Elements.CANCEL_BUTTON_MOBILE.getId()}-mobile">Cancel</button>
                            <button form="delete_form_mobile" id="${Elements.DELETE_BUTTON_MOBILE.getId()}">Delete</button>
                            <button form="simpler_edit_time_panel" id="${Elements.SAVE_BUTTON_MOBILE.getId()}">Save</button>
                        </div>
            </div>
    """
    }

}