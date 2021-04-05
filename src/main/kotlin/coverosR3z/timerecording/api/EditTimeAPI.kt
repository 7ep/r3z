package coverosR3z.timerecording.api

import coverosR3z.authentication.types.Role
import coverosR3z.misc.types.Date
import coverosR3z.server.types.PostEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.timerecording.types.*
import java.lang.IllegalStateException

class EditTimeAPI(private val sd: ServerData) {

    companion object : PostEndpoint {
        override fun handlePost(sd: ServerData): PreparedResponseData {
            val vt = EditTimeAPI(sd)
            return AuthUtilities.doPOSTAuthenticated(
                sd.ahd.user,
                requiredInputs,
                sd.ahd.data,
                Role.REGULAR, Role.APPROVER, Role.ADMIN
            ) { vt.handlePOST() }
        }

        override val requiredInputs = setOf(
            ViewTimeAPI.Elements.PROJECT_INPUT,
            ViewTimeAPI.Elements.TIME_INPUT,
            ViewTimeAPI.Elements.DETAIL_INPUT,
            ViewTimeAPI.Elements.DATE_INPUT,
            ViewTimeAPI.Elements.ID_INPUT,
        )

        override val path: String
            get() = "edittime"
    }


    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data
        val tru = sd.bc.tru
        val projectId = ProjectId.make(data.mapping[ViewTimeAPI.Elements.PROJECT_INPUT.getElemName()])
        val time = Time.makeHoursToMinutes(data.mapping[ViewTimeAPI.Elements.TIME_INPUT.getElemName()])
        val details = Details.make(data.mapping[ViewTimeAPI.Elements.DETAIL_INPUT.getElemName()])
        val date = Date.make(data.mapping[ViewTimeAPI.Elements.DATE_INPUT.getElemName()])
        val entryId = TimeEntryId.make(data.mapping[ViewTimeAPI.Elements.ID_INPUT.getElemName()])

        val project = tru.findProjectById(projectId)
        val employee = tru.findEmployeeById(checkNotNull(sd.ahd.user.employee.id){ employeeIdNotNullMsg })
        if (sd.ahd.user.employee != employee) {
            throw IllegalStateException("It is not allowed for anyone other than the owning employee to edit this time entry")
        }

        val timeEntry = TimeEntry(entryId, employee, project, time, date, details)

        tru.changeEntry(timeEntry)

        val currentPeriod = data.mapping[ViewTimeAPI.Elements.TIME_PERIOD.getElemName()]
        val viewEntriesPage = ViewTimeAPI.path + if (currentPeriod.isNullOrBlank()) "" else "" + "?" + ViewTimeAPI.Elements.TIME_PERIOD.getElemName() + "=" + currentPeriod
        return redirectTo(viewEntriesPage)
    }
}