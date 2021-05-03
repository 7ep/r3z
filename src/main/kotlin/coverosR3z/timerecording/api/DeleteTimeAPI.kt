package coverosR3z.timerecording.api

import coverosR3z.authentication.types.Role
import coverosR3z.server.types.Element
import coverosR3z.server.types.PostEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.timerecording.types.TimeEntryId

class DeleteTimeAPI {

    companion object: PostEndpoint {

        override fun handlePost(sd: ServerData): PreparedResponseData {
            return doPOSTAuthenticated(
                sd.ahd.user,
                requiredInputs,
                sd.ahd.data,
                Role.SYSTEM, Role.ADMIN, Role.APPROVER, Role.REGULAR) {
                    val timeEntryId = TimeEntryId.make(sd.ahd.data.mapping[ViewTimeAPI.Elements.ID_INPUT.getElemName()])
                    val timeEntry = sd.bc.tru.findTimeEntryById(timeEntryId)
                    sd.bc.tru.deleteTimeEntry(timeEntry)

                    val currentPeriod = sd.ahd.data.mapping[ViewTimeAPI.Elements.TIME_PERIOD.getElemName()]
                    val viewEntriesPage = ViewTimeAPI.path + if (currentPeriod.isNullOrBlank()) "" else "" + "?" + ViewTimeAPI.Elements.TIME_PERIOD.getElemName() + "=" + currentPeriod
                    redirectTo(viewEntriesPage)
            }

        }

        override val requiredInputs: Set<Element>
            get() = setOf(ViewTimeAPI.Elements.ID_INPUT)

        override val path: String
            get() = "deletetime"

    }
}