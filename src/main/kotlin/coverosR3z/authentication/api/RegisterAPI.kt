package coverosR3z.authentication.api

import coverosR3z.authentication.types.*
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.GetEndpoint
import coverosR3z.server.types.PostEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireUnauthenticated
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTRequireUnauthenticated
import coverosR3z.timerecording.types.EmployeeId

class RegisterAPI(private val sd: ServerData) {

    enum class Elements(val elemName: String, val id: String) {
        USERNAME_INPUT("username", "username"),
        PASSWORD_INPUT("password", "password"),
        EMPLOYEE_INPUT("employee", "employee"),
        REGISTER_BUTTON("", "register_button"),
    }

    companion object : GetEndpoint, PostEndpoint {

        override val requiredInputs = setOf(
            Elements.USERNAME_INPUT.elemName,
            Elements.PASSWORD_INPUT.elemName,
            Elements.EMPLOYEE_INPUT.elemName,
            Elements.USERNAME_INPUT.elemName,
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
        val data = sd.ahd.data
        val au = sd.au
        val username = UserName.make(data[Elements.USERNAME_INPUT.elemName])
        val password = Password.make(data[Elements.PASSWORD_INPUT.elemName])
        val employeeId = EmployeeId.make(data[Elements.EMPLOYEE_INPUT.elemName])
        val result = au.register(username, password, employeeId)
        return if (result.status == RegistrationResultStatus.SUCCESS) {
            okHTML(successHTML)
        } else {
            okHTML(failureHTML)
        }
    }

    private fun registerHTML() : String {
        val employees = sd.tru.listAllEmployees()

        return """
    <!DOCTYPE html>        
    <html>
      <head>
          <title>register</title>
          <link rel="stylesheet" href="general.css" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <meta apifile="RegisterAPI" >
      </head>
      <header><a class="home-button" href="homepage">r3z</a></header>
      <body>
        <br>
        <h2>Register a User</h2>
        
        <form method="post" action="register">
          <table> 
              <tbody>
                <tr>
                    <td>
                        <label for="${Elements.USERNAME_INPUT.elemName}">Username</label><br>
                        <input type="text" name="${Elements.USERNAME_INPUT.elemName}" id="${Elements.USERNAME_INPUT.id}" minlength="$minUserNameSize" maxlength="$maxUserNameSize" required="required">
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="${Elements.PASSWORD_INPUT.elemName}">Password</label><br>
                        <input type="password" name="${Elements.PASSWORD_INPUT.elemName}" id="${Elements.PASSWORD_INPUT.id}" minlength="$minPasswordSize" maxlength="$maxPasswordSize" required="required">
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="${Elements.EMPLOYEE_INPUT.elemName}">Employee</label><br>
                        <select id="${Elements.EMPLOYEE_INPUT.id}" name="${Elements.EMPLOYEE_INPUT.elemName}" required="required">
                            <option selected disabled hidden value="">Choose here</option>
                           """+employees.joinToString("") { "<option value =\"${it.id.value}\">${safeHtml(it.name.value)}</option>\n" } +"""
                           
                      </td>
                    </select>
                </tr>
                <tr>
                    <td>
                        <button id="${Elements.REGISTER_BUTTON.id}" class="submit">Register</button>
                    </td>
                </tr>
            </tbody>
          </table>
        </form>
      </body>
    </html>
    """
    }

}