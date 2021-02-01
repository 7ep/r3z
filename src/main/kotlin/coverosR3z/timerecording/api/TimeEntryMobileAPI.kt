package coverosR3z.timerecording.api

import coverosR3z.misc.types.Date
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities
import coverosR3z.server.utility.PageComponents
import coverosR3z.timerecording.types.TimeEntry

class TimeEntryMobileAPI(val sd : ServerData) {

    private fun existingTimeEntriesHTML() : String {
        val date = sd.ahd.queryString["date"]
        val employeeId = checkNotNull(sd.ahd.user.employeeId) {"Employee Id must not be null"}
        val validDate = Date.make(date)
        return renderTimeEntryPage(sd.tru.getEntriesForEmployeeOnDate(employeeId, validDate), validDate)
    }

    private fun renderTimeEntryPage(entriesForEmployeeOnDate: Set<TimeEntry>, date : Date): String {
        val body =
"""
<div class="container">
    <div class="date_title">${date.stringValue}</div>
    <div class="new_time_entry_row">
        <button class="project">project</button>
        <button class="notes">...</button>
        <input class="time" type="number" value="" step="0.25"  min="0" max="24" />
        <button class="add_new">CREATE</button>
    </div>  
    ${renderRows(entriesForEmployeeOnDate)}        
    
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
            TODO("Not yet implemented")
        }

        override val requiredInputs: Set<Element>
            get() = TODO("Not yet implemented")

        override val path: String
            get() = "timeentrymobile"

    }
}