package coverosR3z.authentication.api

import coverosR3z.authentication.types.*
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.*
import coverosR3z.server.utility.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireUnauthenticated
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTRequireUnauthenticated
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.timerecording.types.EmployeeId

class RegisterAPI(private val sd: ServerData) {

    enum class Elements(private val elemName: String, private val id: String) : Element {
        USERNAME_INPUT("username", "username"),
        PASSWORD_INPUT("password", "password"),
        EMPLOYEE_INPUT("employee", "employee"),
        REGISTER_BUTTON("", "register_button"),;

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

        override val requiredInputs = setOf(
            Elements.USERNAME_INPUT,
            Elements.PASSWORD_INPUT,
            Elements.EMPLOYEE_INPUT,
            Elements.USERNAME_INPUT,
        )
        override val path: String
            get() = "register"

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val r = RegisterAPI(sd)
            return doGETRequireUnauthenticated(sd.authStatus) { r.registerHTML() }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val r = RegisterAPI(sd)
            return doPOSTRequireUnauthenticated(sd.authStatus, requiredInputs, sd.ahd.data) { r.handlePOST() }
        }
    }

    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data.mapping
        val au = sd.au
        val username = UserName.make(data[Elements.USERNAME_INPUT.getElemName()])
        val password = Password.make(data[Elements.PASSWORD_INPUT.getElemName()])
        val employeeId = EmployeeId.make(data[Elements.EMPLOYEE_INPUT.getElemName()])
        val result = au.register(username, password, employeeId)
        return if (result.status == RegistrationResultStatus.SUCCESS) {
            redirectTo(LoginAPI.path)
        } else {
            okHTML(failureHTML)
        }
    }

    private fun registerHTML() : String {
        val employees = sd.tru.listAllEmployees()

        val body = """
        <h2 role="heading">Register a User</h2>
        
        <form method="post" action="$path">
          <table role="presentation"> 
              <tbody>
                <tr>
                    <td>
                        <label for="${Elements.USERNAME_INPUT.getElemName()}">Username</label><br>
                        <input type="text" name="${Elements.USERNAME_INPUT.getElemName()}" id="${Elements.USERNAME_INPUT.getId()}" minlength="$minUserNameSize" maxlength="$maxUserNameSize" required="required">
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="${Elements.PASSWORD_INPUT.getElemName()}">Password</label><br>
                        <input type="password" name="${Elements.PASSWORD_INPUT.getElemName()}" id="${Elements.PASSWORD_INPUT.getId()}" minlength="$minPasswordSize" maxlength="$maxPasswordSize" required="required">
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="${Elements.EMPLOYEE_INPUT.getElemName()}">Employee</label><br>
                        <select id="${Elements.EMPLOYEE_INPUT.getId()}" name="${Elements.EMPLOYEE_INPUT.getElemName()}" required="required">
                            <option selected disabled hidden value="">Choose here</option>
                           """+employees.joinToString("") { "<option value =\"${it.id.value}\">${safeHtml(it.name.value)}</option>\n" } +"""
                           
                      </td>
                    </select>
                </tr>
                <tr>
                    <td>
                        <button id="${Elements.REGISTER_BUTTON.getId()}" class="submit">Register</button>
                    </td>
                </tr>
            </tbody>
          </table>
        </form>
    """
        return PageComponents.makeTemplate("register", "RegisterAPI", body, extraHeaderContent="""<link rel="stylesheet" href="register.css" />""")
    }

}