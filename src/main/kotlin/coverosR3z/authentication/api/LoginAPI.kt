package coverosR3z.authentication.api

import coverosR3z.authentication.types.LoginResult
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.Password
import coverosR3z.authentication.types.UserName
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.logging.logDebug
import coverosR3z.server.*
import coverosR3z.misc.successHTML
import coverosR3z.server.types.ContentType
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.StatusCode

class LoginAPI {

    enum class Elements(val elemName: String, val id: String)  {
        USERNAME_INPUT("username", "username"),
        PASSWORD_INPUT("password", "password"),
        LOGIN_BUTTON("", "login_button"),
    }

    companion object {

        val requiredInputs = setOf(
            Elements.USERNAME_INPUT.elemName,
            Elements.PASSWORD_INPUT.elemName,
        )

        fun handlePOST(au: IAuthenticationUtilities, data: Map<String, String>) : PreparedResponseData {
            val username = UserName.make(data[Elements.USERNAME_INPUT.elemName])
            val password = Password.make(data[Elements.PASSWORD_INPUT.elemName])
            val (loginResult, loginUser) = au.login(username, password)
            return if (loginResult == LoginResult.SUCCESS && loginUser != NO_USER) {
                val newSessionToken: String = au.createNewSession(loginUser)
                PreparedResponseData(successHTML, StatusCode.OK, listOf(ContentType.TEXT_HTML.value, "Set-Cookie: sessionId=$newSessionToken"))
            } else {
                logDebug { "User ($username) failed to login" }
                handleUnauthorized()
            }
        }

        fun generateLoginPage(): String = loginHTML

        private val loginHTML = """
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
                            <label for="${Elements.USERNAME_INPUT.elemName}">Username</label>
                            <input type="text" name="${Elements.USERNAME_INPUT.elemName}" id="${Elements.USERNAME_INPUT.id}">
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <label for="${Elements.PASSWORD_INPUT.elemName}">Password</label>
                            <input type="password" name="${Elements.PASSWORD_INPUT.elemName}" id="${Elements.PASSWORD_INPUT.id}">
                        </td>
                    </tr>    
                        <td>
                            <button id="${Elements.LOGIN_BUTTON.id}" class="submit">Login</button>
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
