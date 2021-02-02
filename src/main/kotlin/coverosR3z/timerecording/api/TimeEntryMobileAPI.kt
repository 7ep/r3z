package coverosR3z.timerecording.api

import coverosR3z.misc.types.Date
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities
import coverosR3z.server.utility.PageComponents
import coverosR3z.server.utility.ServerUtilities
import coverosR3z.timerecording.types.*

class TimeEntryMobileAPI(val sd : ServerData) {

    private fun existingTimeEntriesHTML() : String {
        val date = sd.ahd.queryString["date"]
        val employeeId = checkNotNull(sd.ahd.user.employeeId) {"Employee Id must not be null"}
        val validDate = Date.make(date)
        val projects = sd.tru.listAllProjects()
        return renderTimeEntryPage(sd.tru.getEntriesForEmployeeOnDate(employeeId, validDate), validDate, projects)
    }

    private fun renderTimeEntryPage(entriesForEmployeeOnDate: Set<TimeEntry>, date : Date, projects: List<Project>): String {
        val body =
"""
<div class="container">
<form action="$path" method="post"> 
    <div class="date_title">${date.stringValue}</div>
    <input name="${ViewTimeAPI.Elements.DATE_INPUT.getElemName()}" class="date_title" value="${Date.now().stringValue}" type="hidden"${date.stringValue}></input>
    <div id="${ViewTimeAPI.Elements.CREATE_TIME_ENTRY_ROW.getId()}" class="new_time_entry_row">
        <select class="project" name="project_entry" id="project_entry" required="required" />
                <option selected disabled hidden value="">Choose here</option>
                ${ViewTimeAPI.projectsToOptions(projects)}
        </select>
        <button class="notes" name="${ViewTimeAPI.Elements.DETAIL_INPUT.getElemName()}" value="..." maxlength="$MAX_DETAILS_LENGTH"/>
        ...
        </button>
        <input class="time" name="${ViewTimeAPI.Elements.TIME_INPUT.getElemName()}" type="number" step="0.25" min="0" max="24"  />
        <button class="${ViewTimeAPI.Elements.SAVE_BUTTON.getElemClass()}">create</button>
    </div>  
    ${renderRows(entriesForEmployeeOnDate)}        
</form>
</div>
"""
        return PageComponents.makeTemplate("time entries", "TimeEntryMobileAPI", body, extraHeaderContent="""<link rel="stylesheet" href="timeentrymobile.css" />""" )
    }

    private fun renderRows(entriesForEmployeeOnDate: Set<TimeEntry>): String {
        return entriesForEmployeeOnDate.joinToString("\n") {
"""
<div class="time_entry_row">
    <button class="project">${it.project.name}</button>
    <button class="notes">...</button>
    <input class="time" type="number" value="${it.time.getHoursAsString()}" step="0.25"  min="0" max="24" />
</div>     
"""
        }
    }


    companion object : GetEndpoint, PostEndpoint  {

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val tem = TimeEntryMobileAPI(sd)
            return AuthUtilities.doGETRequireAuth(sd.authStatus) { tem.existingTimeEntriesHTML() }
        }

        // POST stuff
        override fun handlePost(sd: ServerData): PreparedResponseData {
            val data = sd.ahd.data
            val tru = sd.tru
            val projectId = ProjectId.make(data.mapping[ViewTimeAPI.Elements.PROJECT_INPUT.getElemName()])
            val time = Time.makeHoursToMinutes(data.mapping[ViewTimeAPI.Elements.TIME_INPUT.getElemName()])
            val details = Details.make(data.mapping[ViewTimeAPI.Elements.DETAIL_INPUT.getElemName()])
            val date = Date.make(data.mapping[ViewTimeAPI.Elements.DATE_INPUT.getElemName()])
            val entryId = TimeEntryId.make(data.mapping[ViewTimeAPI.Elements.ID_INPUT.getElemName()])

            val project = tru.findProjectById(projectId)
            val employee = tru.findEmployeeById(checkNotNull(sd.ahd.user.employeeId){ employeeIdNotNullMsg })

            val timeEntry = TimeEntry(entryId, employee, project, time, date, details)
            tru.changeEntry(timeEntry)

            return ServerUtilities.redirectTo(ViewTimeAPI.path)
        }

        override val requiredInputs: Set<Element>
            get() = TODO("Not yet implemented")

        override val path: String
            get() = "timeentrymobile"

    }
}