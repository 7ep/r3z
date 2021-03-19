package coverosR3z.timerecording.api

import coverosR3z.authentication.types.Role
import coverosR3z.misc.types.Date
import coverosR3z.server.types.Element
import coverosR3z.server.types.PostEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.timerecording.types.*

class EnterTimeAPI(private val sd: ServerData) {

    enum class Elements (private val elemName: String, private val id: String) : Element {
        PROJECT_INPUT("project_entry", "project_entry"),
        TIME_INPUT("time_entry", "time_entry"),
        DETAIL_INPUT("detail_entry", "detail_entry"),
        ENTER_TIME_BUTTON("", "enter_time_button"),
        DATE_INPUT("date_entry", "date_entry"),;

        override fun getId(): String {
            return this.id
        }

        override fun getElemName(): String {
            return this.elemName
        }

        override fun getElemClass(): String {
            throw NotImplementedError()
        }
    }

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
            return doPOSTAuthenticated(sd.ahd.user, requiredInputs, sd.ahd.data, Role.REGULAR, Role.APPROVER, Role.ADMIN) { et.handlePOST() }
        }
    }

    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data
        val tru = sd.bc.tru
        val projectId = ProjectId.make(data.mapping[Elements.PROJECT_INPUT.getElemName()])
        val time = Time.makeHoursToMinutes(data.mapping[Elements.TIME_INPUT.getElemName()])
        val details = Details.make(data.mapping[Elements.DETAIL_INPUT.getElemName()])
        val date = Date.make(data.mapping[Elements.DATE_INPUT.getElemName()])

        val project = tru.findProjectById(projectId)
        val employee = checkNotNull(sd.ahd.user.employee){ employeeIdNotNullMsg }

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