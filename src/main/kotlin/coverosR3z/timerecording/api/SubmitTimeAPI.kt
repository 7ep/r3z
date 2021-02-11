package coverosR3z.timerecording.api

import coverosR3z.misc.types.Date
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities
import coverosR3z.server.utility.ServerUtilities
import coverosR3z.timerecording.types.*

class SubmitTimeAPI(private val sd: ServerData){

    enum class Elements (private val elemName: String = "", private val id: String = "", private val elemClass: String = "") : Element {
        START_DATE(elemName = "start_date"),
        END_DATE(elemName = "end_date"),
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

    companion object : PostEndpoint {

        override val requiredInputs = setOf(
            Elements.START_DATE,
            Elements.END_DATE,
        )
        override val path: String
            get() = "submittime"

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val st = SubmitTimeAPI(sd)
            return AuthUtilities.doPOSTAuthenticated(
                sd.authStatus,
                requiredInputs,
                sd.ahd.data
            ) { st.handlePOST() }
        }
    }

    // internal handlePOST() to do the work.
    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data
        val tru = sd.tru // time recording utilities
        val startDate = Date.make(data.mapping[Elements.START_DATE.getElemName()])
        val endDate = Date.make(data.mapping[Elements.END_DATE.getElemName()])
        // val employee = tru.findEmployeeById(checkNotNull(sd.ahd.user.employeeId){ employeeIdNotNullMsg })

        // UNFINISHED: Have not implemented time periods yet.

        // next step: figure out the actual timePeriod for the employee for the start/end dates that has
        // potentially all the time entries that are part of it.
        val timePeriod = TimePeriod(startDate, endDate);

        tru.submitTimePeriod(timePeriod)

        return ServerUtilities.redirectTo(ViewTimeAPI.path)
    }

}