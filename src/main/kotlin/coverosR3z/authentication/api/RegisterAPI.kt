package coverosR3z.authentication.api

import coverosR3z.authentication.types.*
import coverosR3z.misc.utility.safeAttr
import coverosR3z.server.api.handleUnauthorized
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireUnauthenticated
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTRequireUnauthenticated
import coverosR3z.server.utility.PageComponents
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.server.utility.failureHTML

class RegisterAPI(private val sd: ServerData) {

    enum class Elements(private val elemName: String, private val id: String) : Element {
        USERNAME_INPUT("username", "username"),
        PASSWORD_INPUT("password", "password"),
        INVITATION_INPUT("invitation", "invitation"),
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
            Elements.INVITATION_INPUT,
            Elements.USERNAME_INPUT,
        )
        override val path: String
            get() = "register"

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val r = RegisterAPI(sd)
            return doGETRequireUnauthenticated(sd.ahd.user) { r.registerHTML() }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val r = RegisterAPI(sd)
            return doPOSTRequireUnauthenticated(sd.ahd.user, requiredInputs, sd.ahd.data) { r.handlePOST() }
        }
    }

    fun handlePOST() : PreparedResponseData {
        val data = sd.ahd.data.mapping
        val au = sd.bc.au
        val username = UserName.make(data[Elements.USERNAME_INPUT.getElemName()])
        val password = Password.make(data[Elements.PASSWORD_INPUT.getElemName()])
        val invitationCode = InvitationCode.make(data[Elements.INVITATION_INPUT.getElemName()])
        val result = au.register(username, password, invitationCode)
        return when (result.status) {
            RegistrationResultStatus.SUCCESS -> {
                au.removeInvitation(result.user.employee)
                redirectTo(LoginAPI.path)
            }
            RegistrationResultStatus.NO_INVITATION_FOUND -> {
                handleUnauthorized()
            }
            else -> {
                okHTML(failureHTML)
            }
        }
    }

    private fun registerHTML() : String {
        val invitationCode = sd.ahd.queryString["code"]

        val body = """
        <form method="post" action="$path">
        <input type="hidden" name="${Elements.INVITATION_INPUT.getElemName()}" value="${safeAttr(invitationCode)}" />
          <table> 
              <tbody>
                <tr>
                    <td>
                        <label for="${Elements.USERNAME_INPUT.getElemName()}">Username</label>
                        <input type="text" name="${Elements.USERNAME_INPUT.getElemName()}" id="${Elements.USERNAME_INPUT.getId()}" minlength="$minUserNameSize" maxlength="$maxUserNameSize" required="required" />
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="${Elements.PASSWORD_INPUT.getElemName()}">Password</label>
                        <input type="password" name="${Elements.PASSWORD_INPUT.getElemName()}" id="${Elements.PASSWORD_INPUT.getId()}" minlength="$minPasswordSize" maxlength="$maxPasswordSize" required="required" />
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