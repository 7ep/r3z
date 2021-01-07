package coverosR3z.authentication.api

import coverosR3z.authentication.types.Password
import coverosR3z.authentication.types.RegistrationResultStatus
import coverosR3z.authentication.types.UserName
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.utility.failureHTML
import coverosR3z.server.utility.okHTML
import coverosR3z.server.utility.successHTML
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.EmployeeId
import coverosR3z.timerecording.utility.ITimeRecordingUtilities

class RegisterAPI {
    enum class Elements(val elemName: String, val id: String) {
        USERNAME_INPUT("username", "username"),
        PASSWORD_INPUT("password", "password"),
        EMPLOYEE_INPUT("employee", "employee"),
        REGISTER_BUTTON("", "register_button"),
    }

    companion object {

    val requiredInputs = setOf(
        Elements.USERNAME_INPUT.elemName,
        Elements.PASSWORD_INPUT.elemName,
        Elements.EMPLOYEE_INPUT.elemName,
        Elements.USERNAME_INPUT.elemName,
    )

    fun generateRegisterUserPage(tru: ITimeRecordingUtilities): String = registerHTML(tru.listAllEmployees())

    fun handlePOST(au: IAuthenticationUtilities, data: Map<String, String>) : PreparedResponseData {
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

    private fun registerHTML(employees: List<Employee>) : String {
        return """
    <!DOCTYPE html>        
    <html>
      <head>
          <title>register</title>
          <link rel="stylesheet" href="general.css" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
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
                        <input type="text" name="${Elements.USERNAME_INPUT.elemName}" id="${Elements.USERNAME_INPUT.id}">
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="${Elements.PASSWORD_INPUT.elemName}">Password</label><br>
                        <input type="password" name="${Elements.PASSWORD_INPUT.elemName}" id="${Elements.PASSWORD_INPUT.id}">
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="${Elements.EMPLOYEE_INPUT.elemName}">Employee</label><br>
                        <select id="${Elements.EMPLOYEE_INPUT.id}" name="${Elements.EMPLOYEE_INPUT.elemName}">
                            <option selected disabled hidden>Choose here</option>
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

}}