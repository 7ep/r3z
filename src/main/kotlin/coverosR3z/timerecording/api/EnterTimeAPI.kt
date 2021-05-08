package coverosR3z.timerecording.api

import coverosR3z.authentication.types.Role
import coverosR3z.server.api.MessageAPI
import coverosR3z.system.misc.types.Date
import coverosR3z.server.types.PostEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.timerecording.api.ViewTimeAPI.Elements
import coverosR3z.timerecording.types.*
import java.lang.IllegalStateException

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
                sd.ahd.user,
                requiredInputs,
                sd.ahd.data,
                Role.REGULAR, Role.APPROVER, Role.ADMIN) {
                et.handlePOST()
            }
        }
    }

    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data
        val tru = sd.bc.tru
        val projectName = ProjectName.make(data.mapping[Elements.PROJECT_INPUT.getElemName()])
        val project = tru.findProjectByName(projectName)
        if (project == NO_PROJECT) {
            return MessageAPI.createMessageRedirect(MessageAPI.Message.INVALID_PROJECT_DURING_ENTERING_TIME)
        }
        val timeInputString = data.mapping[Elements.TIME_INPUT.getElemName()]
        val time = Time.makeHoursToMinutes(timeInputString)
        check(time.numberOfMinutes % 5 == 0) { "number of minutes must be multiple of 15.  Yours was ${time.getHoursAsString()} hours or ${time.numberOfMinutes} minutes" }
        check(time.numberOfMinutes <= 24 * 60) { "Not able to enter more than 24 hours on a daily entry.  You entered ${time.getHoursAsString()} hours" }
        val details = Details.make(data.mapping[Elements.DETAIL_INPUT.getElemName()])
        val date = Date.make(data.mapping[Elements.DATE_INPUT.getElemName()])

        val employee = checkNotNull(sd.ahd.user.employee){ employeeIdNotNullMsg }
        if (sd.ahd.user.employee != employee) {
            throw IllegalStateException("It is not allowed for other employees to enter this employee's time")
        }
        check(! tru.isInASubmittedPeriod(employee, date)) { "A new time entry is not allowed in a submitted time period" }

        val timeEntry = TimeEntryPreDatabase(
                employee,
                project,
                time,
                date,
                details)

        tru.createTimeEntry(timeEntry)

        return redirectTo(ViewTimeAPI.path + "?date=" + date.stringValue)
    }

}