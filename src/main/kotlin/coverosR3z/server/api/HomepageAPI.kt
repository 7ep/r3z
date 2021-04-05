package coverosR3z.server.api

import coverosR3z.authentication.api.LoginAPI
import coverosR3z.authentication.exceptions.UnpermittedOperationException
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.utility.RolesChecker
import coverosR3z.misc.utility.safeAttr
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.AuthStatus
import coverosR3z.server.types.GetEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities.Companion.isAuthenticated
import coverosR3z.server.utility.PageComponents
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.timerecording.api.ViewTimeAPI

class HomepageAPI(private val sd: ServerData)  {

    companion object : GetEndpoint {

        override fun handleGet(sd: ServerData) : PreparedResponseData {
            val hp = HomepageAPI(sd)
            return try {
                when (isAuthenticated(sd.ahd.user)) {
                    AuthStatus.AUTHENTICATED -> {
                        val cu = CurrentUser(sd.ahd.user)
                        RolesChecker(cu).checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
                        if (cu.role in listOf(Role.REGULAR, Role.APPROVER)) {
                            redirectTo(ViewTimeAPI.path)
                        } else {
                            okHTML(hp.authHomePageHTML())
                        }
                    }
                    AuthStatus.UNAUTHENTICATED -> redirectTo(LoginAPI.path)
                }
            } catch (ex: UnpermittedOperationException) {
                handleUnauthorized(ex.message)
            }
            /**
             * This is the method for when we want to go either one direction
             * if authenticated or another if unauthenticated.  Most likely
             * example: the homepage
             */
        }

        override val path: String
            get() = "homepage"
    }

    private fun authHomePageHTML(): String {
        val body = renderHomepageBody()

        return PageComponents(sd).makeTemplate("Authenticated Homepage", "HomepageAPI", body, extraHeaderContent="""<link rel="stylesheet" href="authhomepage.css" />""")
    }

    data class HomepageItem(val link: String, val descr: String)

    private fun renderHomepageBody(): String {
        val user = sd.ahd.user
        val allowedToSee: List<HomepageItem> = when (user.role) {
            Role.ADMIN ->
                listOf(
                    HomepageItem("createemployee", "Create employee"),
                    HomepageItem("createproject", "Create project"),
                    HomepageItem("timeentries", "Time entries"),
                    HomepageItem("logging", "Log configuration"),
                )
            Role.APPROVER,
            Role.REGULAR ->
                listOf(
                    HomepageItem("timeentries", "Time entries"),
                )

            Role.SYSTEM,
            Role.NONE -> emptyList()

        }
        return """
        <nav>
            <ul>
                ${allowedToSee.joinToString("") { renderHomepageItem(it.link, it.descr) }}
            </ul>
        </nav>
        """
    }
    private fun renderHomepageItem(linkTarget: String, description: String): String {

        return """
        <li><a href="${safeAttr(linkTarget)}">${safeHtml(description)}</a></li>
        """
    }

}