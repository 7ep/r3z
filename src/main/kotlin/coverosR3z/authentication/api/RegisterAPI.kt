package coverosR3z.authentication.api

import coverosR3z.authentication.types.*
import coverosR3z.system.config.SIZE_OF_DECENT_PASSWORD
import coverosR3z.system.misc.utility.generateRandomString
import coverosR3z.system.misc.utility.safeAttr
import coverosR3z.server.api.HomepageAPI
import coverosR3z.server.api.handleUnauthorized
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireUnauthenticated
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTRequireUnauthenticated
import coverosR3z.server.utility.PageComponents
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.timerecording.types.NO_EMPLOYEE

class RegisterAPI(private val sd: ServerData) {

    enum class Elements(private val elemName: String, private val id: String) : Element {
        USERNAME_INPUT("username", "username"),
        NEW_PASSWORD("", "new_password"),

        REGISTER_BUTTON("", "register_button"),

        // Query string keys

        //the invitation code
        INVITATION_INPUT("invitation", ""),

        // the message to show if there was a problem with the username
        ERROR_MESSAGE("msg", "")
        ;

        override fun getId(): String {
            return this.id
        }

        override fun getElemName(): String {
            return this.elemName
        }

        override fun getElemClass(): String {
            throw IllegalAccessError()
        }
    }

    companion object : GetEndpoint, PostEndpoint {

        override val requiredInputs = setOf(
            Elements.USERNAME_INPUT,
            Elements.INVITATION_INPUT,
            Elements.USERNAME_INPUT,
        )

        override val path = "register"

        // potential errors
        const val duplicateUser = "duplicate_user"
        const val invalidUsername = "invalid_name"

        override fun handleGet(sd: ServerData): PreparedResponseData {
            // we won't even allow the user to see the register page unless they walk
            // in with a completely valid invitation code.  None of this half-cooked blarney.
            val invitationCode = sd.ahd.queryString[Elements.INVITATION_INPUT.getElemName()] ?: return redirectTo(HomepageAPI.path)
            if (sd.bc.au.getEmployeeFromInvitationCode(InvitationCode(invitationCode)) == NO_EMPLOYEE) return redirectTo(HomepageAPI.path)

            val r = RegisterAPI(sd)
            return doGETRequireUnauthenticated(sd.ahd.user) { r.registerHTML(invitationCode) }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val r = RegisterAPI(sd)
            return doPOSTRequireUnauthenticated(sd.ahd.user, requiredInputs, sd.ahd.data) { r.handlePOST() }
        }
    }

    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data.mapping
        val au = sd.bc.au
        val invitationCode = InvitationCode.make(data[Elements.INVITATION_INPUT.getElemName()])
        val username = try {
            extractUsername(data)
        } catch (ex: Throwable) {
            return redirectTo("$path?${Elements.INVITATION_INPUT.getElemName()}=${invitationCode.value}&${Elements.ERROR_MESSAGE.getElemName()}=$invalidUsername")
        }
        val password = Password(generateRandomString(SIZE_OF_DECENT_PASSWORD))
        val result = au.register(username, password, invitationCode)
        return when (result.status) {
            RegistrationResultStatus.SUCCESS -> {
                au.removeInvitation(result.user.employee)
                val newUserHtml = """
                <div class="container">
                    <p>
                        Hello ${username.value}!
                    </p>
                    <p>
                        Your new password is <span id="${Elements.NEW_PASSWORD.getId()}">${password.value}</span>
                    </p>
                    <p>
                        <em>store this somewhere, like a secure password manager.</em>
                    </p>
                    <p><a href="${HomepageAPI.path}">Homepage</a></p>
                </div>
                """
                okHTML(PageComponents(sd).makeTemplate("New password generated", "RegisterAPI", newUserHtml, extraHeaderContent="""<link rel="stylesheet" href="auth.css" />"""))
            }
            RegistrationResultStatus.NO_INVITATION_FOUND -> {
                handleUnauthorized()
            }
            RegistrationResultStatus.USERNAME_ALREADY_REGISTERED -> {
                redirectTo("$path?${Elements.INVITATION_INPUT.getElemName()}=${invitationCode.value}&${Elements.ERROR_MESSAGE.getElemName()}=$duplicateUser")
            }
        }
    }

    private fun extractUsername(data: Map<String, String>): UserName {
        val usernameString = checkNotNull(data[Elements.USERNAME_INPUT.getElemName()])
        val usernameTrimmed = usernameString.trim()
        return UserName(usernameTrimmed)
    }

    private fun registerHTML(invitationCode: String): String {
        val errorMsg = sd.ahd.queryString[Elements.ERROR_MESSAGE.getElemName()]
        val renderedError = when (errorMsg) {
            duplicateUser -> """ <div id="error_message">The chosen username has already been selected.  Please choose another</div>"""
            invalidUsername -> """ <div id="error_message">The name must be between $minUserNameSize and $maxUserNameSize chars</div>"""
            else -> ""
        }
        val body = """
        $renderedError
        <form method="post" action="$path">
        <input type="hidden" name="${Elements.INVITATION_INPUT.getElemName()}" value="${safeAttr(invitationCode)}" />
          <table> 
              <tbody>
                <tr>
                    <td>
                        <label for="${Elements.USERNAME_INPUT.getElemName()}">Username</label>
                        <input autocomplete="off" autofocus type="text" name="${Elements.USERNAME_INPUT.getElemName()}" id="${Elements.USERNAME_INPUT.getId()}" minlength="$minUserNameSize" maxlength="$maxUserNameSize" pattern="[\s\S]*\S[\s\S]*" required="required" oninvalid="this.setCustomValidity('Enter three or more non-whitespace characters')" oninput="this.setCustomValidity('')" />
                    </td>
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
        return PageComponents(sd).makeTemplate("register", "RegisterAPI", body, extraHeaderContent="""<link rel="stylesheet" href="auth.css" />""")
    }

}