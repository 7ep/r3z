package coverosR3z.authentication

import coverosR3z.domainobjects.*
import coverosR3z.misc.checkHasExactInputs
import coverosR3z.server.*
import coverosR3z.timerecording.ITimeRecordingUtilities
import coverosR3z.misc.failureHTML
import coverosR3z.misc.safeHtml
import coverosR3z.misc.successHTML

enum class RegisterElements(val elemName: String, val id: String) {
    USERNAME_INPUT("username", "username"),
    PASSWORD_INPUT("password", "password"),
    EMPLOYEE_INPUT("employee", "employee"),
    REGISTER_BUTTON("", "register_button");
}

private val requiredInputs = setOf(
    RegisterElements.USERNAME_INPUT.elemName,
    RegisterElements.PASSWORD_INPUT.elemName,
    RegisterElements.EMPLOYEE_INPUT.elemName,
    RegisterElements.USERNAME_INPUT.elemName,
)

fun generateRegisterUserPage(tru: ITimeRecordingUtilities): String = registerHTML(tru.listAllEmployees())

fun handlePOSTRegister(au: IAuthenticationUtilities, user: User, data: Map<String, String>) : PreparedResponseData {
    return if (user == NO_USER) {
        checkHasExactInputs(data.keys, requiredInputs)
        val username = UserName.make(data[RegisterElements.USERNAME_INPUT.elemName])
        val password = Password.make(data[RegisterElements.PASSWORD_INPUT.elemName])
        val employeeId = EmployeeId.make(data[RegisterElements.EMPLOYEE_INPUT.elemName])
        val result = au.register(username, password, employeeId)
        if (result == RegistrationResult.SUCCESS) {
            okHTML(successHTML)
        } else {
            okHTML(failureHTML)
        }
    } else {
        redirectTo(NamedPaths.AUTHHOMEPAGE.path)
    }
}

fun registerHTML(employees: List<Employee>) : String {
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
                    <label for="${RegisterElements.USERNAME_INPUT.elemName}">Username</label><br>
                    <input type="text" name="${RegisterElements.USERNAME_INPUT.elemName}" id="${RegisterElements.USERNAME_INPUT.id}">
                </td>
            </tr>
            <tr>
                <td>
                    <label for="${RegisterElements.PASSWORD_INPUT.elemName}">Password</label><br>
                    <input type="password" name="${RegisterElements.PASSWORD_INPUT.elemName}" id="${RegisterElements.PASSWORD_INPUT.id}">
                </td>
            </tr>
            <tr>
                <td>
                    <label for="${RegisterElements.EMPLOYEE_INPUT.elemName}">Employee</label><br>
                    <select id="${RegisterElements.EMPLOYEE_INPUT.id}" name="${RegisterElements.EMPLOYEE_INPUT.elemName}">
                       """+employees.joinToString("") { "<option value =\"${it.id.value}\">${safeHtml(it.name.value)}</option>\n" } +"""
                       <option selected disabled hidden>Choose here</option>
                  </td>
                </select>
            </tr>
            <tr>
                <td>
                    <button id="${RegisterElements.REGISTER_BUTTON.id}" class="submit">Register</button>
                </td>
            </tr>
        </tbody>
      </table>
    </form>
  </body>
</html>
"""
}