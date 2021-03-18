package coverosR3z.server.api

import coverosR3z.authentication.types.Role
import coverosR3z.misc.utility.safeAttr
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.GetEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities.Companion.doGETAuthAndUnauth
import coverosR3z.server.utility.PageComponents

class HomepageAPI(private val sd: ServerData)  {

    companion object : GetEndpoint {

        override fun handleGet(sd: ServerData) : PreparedResponseData {
            val hp = HomepageAPI(sd)
            return doGETAuthAndUnauth(sd.ahd.user,
                Role.REGULAR, Role.APPROVER, Role.ADMIN,
                generatorAuthenticated = { hp.authHomePageHTML() },
                generatorUnauth = { PageComponents(sd).makeTemplate("Homepage", "HomepageAPI", "", extraHeaderContent="""<link rel="stylesheet" href="homepage.css" />""")  })
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