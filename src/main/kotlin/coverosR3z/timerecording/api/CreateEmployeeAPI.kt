package coverosR3z.timerecording.api

import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.server.utility.PageComponents
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.successHTML
import coverosR3z.timerecording.types.EmployeeName

class CreateEmployeeAPI(private val sd: ServerData) {

    enum class Elements(private val elemName: String, private val id: String) : Element {
        EMPLOYEE_INPUT("employee_name", "employee_name"),
        CREATE_BUTTON("", "employee_create_button");

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

    companion object : GetEndpoint, PostEndpoint {

        /**
         * The required inputs for this API
         */
        override val requiredInputs = setOf(Elements.EMPLOYEE_INPUT)

        override val path: String
            get() = "createemployee"

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val ce = CreateEmployeeAPI(sd)
            return doGETRequireAuth(sd.authStatus) { ce.createEmployeeHTML() }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val ce = CreateEmployeeAPI(sd)
            return doPOSTAuthenticated(sd.authStatus, requiredInputs, sd.ahd.data) { ce.createEmployee() }
        }

    }

    fun createEmployee() : PreparedResponseData {
        sd.bc.tru.createEmployee(EmployeeName.make(sd.ahd.data.mapping[Elements.EMPLOYEE_INPUT.getElemName()]))
        return okHTML(successHTML)
    }

    private fun createEmployeeHTML() : String {
        val username = safeHtml(sd.ahd.user.name.value)

        val body = """
        <form action="$path" method="post">
        
            <p>
                Hello there, <span id="username">$username</span>!
            </p>
        
            <p>
                <label for="${Elements.EMPLOYEE_INPUT.getElemName()}">Name:</label>
                <input name="${Elements.EMPLOYEE_INPUT.getElemName()}" id="${Elements.EMPLOYEE_INPUT.getId()}" type="text" />
            </p>
        
            <p>
                <button id="${Elements.CREATE_BUTTON.getId()}">Create new employee</button>
            </p>
        
        </form>
    """
        return PageComponents.makeTemplate("create employee", "CreateEmployeeAPI", body, extraHeaderContent="""<link rel="stylesheet" href="createemployee.css" />""")
    }
}
