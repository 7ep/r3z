package coverosR3z.authentication

import coverosR3z.domainobjects.*
import coverosR3z.logging.logInfo
import coverosR3z.server.*
import coverosR3z.misc.successHTML

fun handlePOSTLogin(au: IAuthenticationUtilities, user: User, data: Map<String, String>) : PreparedResponseData {
    val isUnauthenticated = user == NO_USER
    return if (isUnauthenticated) {
        val username = UserName.make(data["username"])
        val password = Password.make(data["password"])
        val (loginResult, loginUser) = au.login(username, password)
        if (loginResult == LoginResult.SUCCESS && loginUser != NO_USER) {
            val newSessionToken: String = au.createNewSession(loginUser)
            PreparedResponseData(successHTML, ResponseStatus.OK, listOf(ContentType.TEXT_HTML.value, "Set-Cookie: sessionId=$newSessionToken"))
        } else {
            logInfo("User ($username) failed to login")
            handleUnauthorized()
        }
    } else {
        redirectTo(NamedPaths.AUTHHOMEPAGE.path)
    }
}

fun doGETLoginPage(rd: RequestData): PreparedResponseData {
    return if (isAuthenticated(rd)) {
        redirectTo(NamedPaths.AUTHHOMEPAGE.path)
    } else {
        okHTML(loginHTML)
    }
}

const val loginHTML = """
<!DOCTYPE html>
<html>
  <head>
      <link href="https://fonts.googleapis.com/css?family=Ubuntu" rel="stylesheet">
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
                    <label for="username">Username</label>
                    <input type="text" name="username" id="username">
                </td>
            </tr>
            <tr>
                <td>
                    <label for="password">Password</label>
                    <input type="password" name="password" id="password">
                </td>
            </tr>    
                <td>
                    <button id="login_button" class="submit">Login</button>
                </td>
            </tr>
        </tbody>
      </table>
    </form>
  </body>
</html>      
"""
