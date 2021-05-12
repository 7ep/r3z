package coverosR3z.timerecording.api

import coverosR3z.authentication.types.Role
import coverosR3z.system.misc.types.Date
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities
import coverosR3z.server.utility.ServerUtilities
import coverosR3z.timerecording.types.*
import java.lang.IllegalStateException

class SubmitTimeAPI(private val sd: ServerData){

    enum class Elements (private val elemName: String = "", private val id: String = "", private val elemClass: String = "") : Element {
        START_DATE(elemName = "start_date"),
        UNSUBMIT(elemName = "unsubmit")
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
            Elements.UNSUBMIT
        )
        override val path: String
            get() = "submittime"

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val st = SubmitTimeAPI(sd)
            return AuthUtilities.doPOSTAuthenticated(
                sd,
                requiredInputs,
                ViewTimeAPI.path,
                Role.REGULAR, Role.APPROVER, Role.ADMIN
            ) { st.handlePOST() }
        }
    }

    // internal handlePOST() to do the work.
    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data
        val tru = sd.bc.tru // time recording utilities
        val unsubmitting = data.mapping[Elements.UNSUBMIT.getElemName()] == "true"
        val submitting = ! unsubmitting
        val employee = sd.ahd.user.employee

        val dateQueryString = data.mapping[Elements.START_DATE.getElemName()]
        val startDate = try {
            Date.make(dateQueryString)
        } catch (ex: Throwable) {
            throw IllegalStateException("""The date for submitting time was not interpreted as a date. You sent "$dateQueryString".  Format is YYYY-MM-DD""")
        }
        val timePeriod = TimePeriod.getTimePeriodForDate(startDate)

        check (tru.isApproved(employee, timePeriod.start) != ApprovalStatus.APPROVED) {"""This time period is approved.  Cannot operate on approved time periods."""}
        if (tru.isInASubmittedPeriod(employee, timePeriod.start) && submitting) {
            throw IllegalStateException("This time period is already submitted.  Cannot submit on this period again.")
        }

        if (unsubmitting) {
            tru.unsubmitTimePeriod(timePeriod)
        } else {
            tru.submitTimePeriod(timePeriod)
        }


        return ServerUtilities.redirectTo(ViewTimeAPI.path +  "?" + ViewTimeAPI.Elements.TIME_PERIOD.getElemName() + "=" + startDate.stringValue)
    }

}