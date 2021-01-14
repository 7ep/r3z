package coverosR3z.timerecording.api

import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.successHTML
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
    }

    companion object : GetEndpoint, PostEndpoint {

        override val requiredInputs = setOf(
            Elements.PROJECT_INPUT,
            Elements.TIME_INPUT,
            Elements.DETAIL_INPUT,
            Elements.DATE_INPUT,
        )
        override val path: String
            get() = "entertime"

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val et = EnterTimeAPI(sd)
            return doGETRequireAuth(sd.authStatus) { et.entertimeHTML() }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val et = EnterTimeAPI(sd)
            return doPOSTAuthenticated(sd.authStatus, requiredInputs, sd.ahd.data) { et.handlePOST() }
        }
    }





    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data
        val tru = sd.tru
        val projectId = ProjectId.make(data.mapping[Elements.PROJECT_INPUT.getElemName()])
        val time = Time.make(data.mapping[Elements.TIME_INPUT.getElemName()])
        val details = Details.make(data.mapping[Elements.DETAIL_INPUT.getElemName()])
        val date = Date.make(data.mapping[Elements.DATE_INPUT.getElemName()])

        val project = tru.findProjectById(projectId)
        val employee = tru.findEmployeeById(checkNotNull(sd.ahd.user.employeeId){ employeeIdNotNullMsg })

        val timeEntry = TimeEntryPreDatabase(
                employee,
                project,
                time,
                date,
                details)

        tru.recordTime(timeEntry)

        return okHTML(successHTML)
    }


    private fun entertimeHTML() : String {
        val username = safeHtml(sd.ahd.user.name.value)
        val projects = sd.tru.listAllProjects()

        return """
        <!DOCTYPE html>        
        <html>
        <head>
            <title>enter time</title>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta apifile="EnterTimeAPI" >
            <link rel="stylesheet" href="entertime.css" />
            <script src="entertime.js"></script>
        </head>
        <body>
            <form action="$path" method="post">
    
                <p>
                    Hello there, <span id="username">$username</span>!
                </p>
    
                <p>
                    <label for="project_entry">Project:</label>
                    <select name="project_entry" id="project_entry"/>
                    """ + projects.joinToString("") { "<option value =\"${it.id.value}\">${safeHtml(it.name.value)}</option>\n" } +"""             <option selected disabled hidden>Choose here</option>
                </select>
                </p>
    
                <p>
                    <label for="${Elements.TIME_INPUT.getElemName()}">Time:</label>
                    <input name="${Elements.TIME_INPUT.getElemName()}" id="${Elements.TIME_INPUT.getId()}" type="text" />
                </p>
    
                <p>
                    <label for="${Elements.DETAIL_INPUT.getElemName()}">Details:</label>
                    <input name="${Elements.DETAIL_INPUT.getElemName()}" id="${Elements.DETAIL_INPUT.getId()}" type="text" />
                </p>
                
                <p>
                    <label for="${Elements.DATE_INPUT.getElemName()}">Date:</label>
                    <input name="${Elements.DATE_INPUT.getElemName()}" id="${Elements.DATE_INPUT.getId()}" type="date" />
                </p>
                
                <p>
                    <button id="${Elements.ENTER_TIME_BUTTON.getId()}">Enter time</button>
                </p>
    
            </form>
        </body>
    </html>
    """
    }

}