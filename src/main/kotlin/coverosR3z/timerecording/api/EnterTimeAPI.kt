package coverosR3z.timerecording.api

import coverosR3z.authentication.types.Role
import coverosR3z.server.api.MessageAPI
import coverosR3z.server.types.PostEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.system.misc.types.Date
import coverosR3z.timerecording.api.ViewTimeAPI.Elements
import coverosR3z.timerecording.types.*

class EnterTimeAPI(private val sd: ServerData) {

    companion object : PostEndpoint {

        override val requiredInputs = setOf(
            Elements.PROJECT_INPUT,
            Elements.TIME_INPUT,
            Elements.DETAIL_INPUT,
            Elements.DATE_INPUT,
        )
        override val path: String
            get() = "entertime"

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val et = EnterTimeAPI(sd)
            return doPOSTAuthenticated(
                sd,
                requiredInputs,
                ViewTimeAPI.path,
                Role.REGULAR, Role.APPROVER, Role.ADMIN) {
                et.handlePOST()
            }
        }
    }

    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data
        val tru = sd.bc.tru
        val employee = sd.ahd.user.employee
        val twentyFourHours = 24 * 60

        // get the project
        val projectName = ProjectName.make(data.mapping[Elements.PROJECT_INPUT.getElemName()])
        val project = tru.findProjectByName(projectName)

        val details = Details.make(data.mapping[Elements.DETAIL_INPUT.getElemName()])

        val date = Date.make(data.mapping[Elements.DATE_INPUT.getElemName()])

        // get the time entered
        val timeInputString = data.mapping[Elements.TIME_INPUT.getElemName()]
        val time = Time.makeHoursToMinutes(timeInputString)

        // confirm the project is valid
        if (project == NO_PROJECT) return MessageAPI.createEnumMessageRedirect(MessageAPI.Message.INVALID_PROJECT_DURING_ENTERING_TIME)

        // confirm the time entered is valid
        if (time.numberOfMinutes % 30 != 0) return MessageAPI.createEnumMessageRedirect(MessageAPI.Message.MINUTES_MUST_BE_MULTIPLE_OF_HALF_HOUR)

        if (time.numberOfMinutes > twentyFourHours) return MessageAPI.createEnumMessageRedirect(MessageAPI.Message.TIME_MUST_BE_LESS_OR_EQUAL_TO_24)

        // confirm we're not entering time in a submitted period
        if(tru.isInASubmittedPeriod(employee, date)) return MessageAPI.createEnumMessageRedirect(MessageAPI.Message.NO_TIME_ENTRY_ALLOWED_IN_SUBMITTED_PERIOD)

        // enter or edit the time entry
        if (data.mapping[Elements.BEING_EDITED.getElemName()].toBoolean()) {
            val entryId = TimeEntryId.make(data.mapping[Elements.ID_INPUT.getElemName()])
            val timeEntry = TimeEntry(entryId, employee, project, time, date, details)
            tru.changeEntry(timeEntry)
        } else {
            // set up the time entry object
            val timeEntry = TimeEntryPreDatabase(
                employee,
                project,
                time,
                date,
                details)
            tru.createTimeEntry(timeEntry)
        }


        return redirectTo(ViewTimeAPI.path + "?date=" + date.stringValue)
    }

}