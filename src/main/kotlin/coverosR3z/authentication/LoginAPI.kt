package coverosR3z.authentication

import coverosR3z.domainobjects.*
import coverosR3z.logging.logDebug
import coverosR3z.server.*
import coverosR3z.misc.successHTML

enum class LoginElements(val elemName: String, val id: String) {
    USERNAME_INPUT("username", "username"),
    PASSWORD_INPUT("password", "password"),
    LOGIN_BUTTON("", "login_button");
}

fun handlePOSTLogin(au: IAuthenticationUtilities, user: User, data: Map<String, String>) : PreparedResponseData {
    val isUnauthenticated = user == NO_USER
    return if (isUnauthenticated) {
        val username = UserName.make(data[LoginElements.USERNAME_INPUT.elemName])
        val password = Password.make(data[LoginElements.PASSWORD_INPUT.elemName])
        val (loginResult, loginUser) = au.login(username, password)
        if (loginResult == LoginResult.SUCCESS && loginUser != NO_USER) {
            val newSessionToken: String = au.createNewSession(loginUser)
            PreparedResponseData(successHTML, StatusCode.OK, listOf(ContentType.TEXT_HTML.value, "Set-Cookie: sessionId=$newSessionToken"))
        } else {
            logDebug("User ($username) failed to login")
            handleUnauthorized()
        }
    } else {
        redirectTo(NamedPaths.AUTHHOMEPAGE.path)
    }
}

fun doGETLoginPage(rd: AnalyzedHttpData): PreparedResponseData {
    return if (isAuthenticated(rd)) {
        redirectTo(NamedPaths.AUTHHOMEPAGE.path)
    } else {
        okHTML(loginHTML)
    }
}

val loginHTML = """
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
    <h2>Login</h2>
    
    <form  method="post" action="login">
      <table> 
        <tbody>
            <tr>
                <td>
                    <label for="${LoginElements.USERNAME_INPUT.elemName}">Username</label>
                    <input type="text" name="${LoginElements.USERNAME_INPUT.elemName}" id="${LoginElements.USERNAME_INPUT.id}">
                </td>
            </tr>
            <tr>
                <td>
                    <label for="${LoginElements.PASSWORD_INPUT.elemName}">Password</label>
                    <input type="password" name="${LoginElements.PASSWORD_INPUT.elemName}" id="${LoginElements.PASSWORD_INPUT.id}">
                </td>
            </tr>    
                <td>
                    <button id="${LoginElements.LOGIN_BUTTON.id}" class="submit">Login</button>
                </td>
            </tr>
        </tbody>
      </table>
    </form>
  </body>
</html>      
"""
