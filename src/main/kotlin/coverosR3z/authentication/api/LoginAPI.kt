package coverosR3z.authentication.api

import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.server.api.HomepageAPI
import coverosR3z.server.api.handleUnauthorized
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireUnauthenticated
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTRequireUnauthenticated
import coverosR3z.server.utility.PageComponents

class LoginAPI(val sd: ServerData) {

    enum class Elements(private val elemName: String, private val id: String) : Element  {
        USERNAME_INPUT("username", "username"),
        PASSWORD_INPUT("password", "password"),
        LOGIN_BUTTON("", "login_button"),;

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
        )
        override val path: String
            get() = "login"

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val l = LoginAPI(sd)
            return doGETRequireUnauthenticated(sd.authStatus)
            { PageComponents.makeTemplate("login page", "LoginAPI", l.loginHTML, extraHeaderContent="""<link rel="stylesheet" href="loginpage.css" />""")}
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val l = LoginAPI(sd)
            return doPOSTRequireUnauthenticated(sd.authStatus, requiredInputs, sd.ahd.data) { l.handlePOST() }
        }
    }

    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data.mapping
        val au = sd.bc.au
        val username = UserName.make(data[Elements.USERNAME_INPUT.getElemName()])
        val password = Password.make(data[Elements.PASSWORD_INPUT.getElemName()])
        val (loginResult, loginUser) = au.login(username, password)
        return if (loginResult == LoginResult.SUCCESS && loginUser != NO_USER) {

            val cookie = createCookie(au, loginUser)

            PreparedResponseData("", StatusCode.SEE_OTHER, listOf(cookie, "Location: ${HomepageAPI.path}"))
        } else {
            sd.logger.logDebug { "User (${username.value}) failed to login" }
            handleUnauthorized()
        }
    }

    /**
     * Builds a secure cookie to enable secure sessions
     */
    private fun createCookie(
        au: IAuthenticationUtilities,
        loginUser: User
    ): String {
        /**
         * we use SameSite as a way to avoid cross-site scripting attacks.
         * see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite
         */
        val sameSiteAttribute = "SameSite=Strict"

        /**
         * HttpOnly - JavaScript cannot access this cookie
         * see https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies#restrict_access_to_cookies
         */
        val httpOnlyAttribute = "HttpOnly"

        /**
         * Secure - cookie is only sent when a request is made with https,
         * except on localhost.
         * See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie#Secure
         * Note that this is a highly-intrusive attribute... adding this will
         * cause the system not to work unless you are on an https endpoint.
         */
        val secureAttribute = "Secure"

        /**
         * This is the actual tracking code we generate per session
         */
        val actualTrackingCode = "sessionId=${au.createNewSession(loginUser)}"

        return "Set-Cookie: $actualTrackingCode; $sameSiteAttribute; $httpOnlyAttribute; $secureAttribute"
    }

    private val loginHTML = """
<h2>Login</h2>

<form method="post" action="$path">
  <table role="presentation"> 
    <tbody>
        <tr>
            <td>
                <label for="${Elements.USERNAME_INPUT.getElemName()}">Username</label>
                <input type="text" name="${Elements.USERNAME_INPUT.getElemName()}" id="${Elements.USERNAME_INPUT.getId()}" minlength="$minUserNameSize" maxlength="$maxUserNameSize" required>
            </td>
        </tr>
        <tr>
            <td>
                <label for="${Elements.PASSWORD_INPUT.getElemName()}">Password</label>
                <input type="password" name="${Elements.PASSWORD_INPUT.getElemName()}" id="${Elements.PASSWORD_INPUT.getId()}" minlength="$minPasswordSize" maxlength="$maxPasswordSize" required>
            </td>
        </tr>    
            <td>
                <button id="${Elements.LOGIN_BUTTON.getId()}" class="submit">Login</button>
            </td>
        </tr>
    </tbody>
  </table>
</form>
"""

}
