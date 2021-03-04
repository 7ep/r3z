package coverosR3z.server.api

import coverosR3z.authentication.types.Roles
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
            return doGETAuthAndUnauth(sd.authStatus, { hp.authHomePageHTML() },
                { PageComponents.makeTemplate("Homepage", "HomepageAPI", hp.homepageHTML, extraHeaderContent="""<link rel="stylesheet" href="homepage.css" />""")  })
        }

        override val path: String
            get() = "homepage"
    }

    private fun authHomePageHTML(): String {
        val body = renderHomepageBody()

        return PageComponents.makeTemplate("Authenticated Homepage", "HomepageAPI", body, extraHeaderContent="""<link rel="stylesheet" href="authhomepage.css" />""")
    }

    data class HomepageItem(val link: String, val descr: String)

    private fun renderHomepageBody(): String {
        val user = sd.ahd.user
        val allowedToSee: List<HomepageItem> = when (user.role) {
            Roles.ADMIN ->
                listOf(
                    HomepageItem("createemployee", "Create employee"),
                    HomepageItem("employees", "Show all employees"),
                    HomepageItem("createproject", "Create project"),
                    HomepageItem("entertime", "Enter time"),
                    HomepageItem("timeentries", "Show all time entries"),
                    HomepageItem("logging", "Log configuration"),
                    HomepageItem("logout", "Logout")
                )
            Roles.REGULAR ->
                listOf(
                    HomepageItem("employees", "Show all employees"),
                    HomepageItem("entertime", "Enter time"),
                    HomepageItem("timeentries", "Show all time entries"),
                    HomepageItem("logging", "Log configuration"),
                    HomepageItem("logout", "Logout")
                )

            Roles.APPROVER ->
                listOf(
                    HomepageItem("employees", "Show all employees"),
                    HomepageItem("entertime", "Enter time"),
                    HomepageItem("timeentries", "Show all time entries"),
                    HomepageItem("logging", "Log configuration"),
                    HomepageItem("logout", "Logout")
                )
            Roles.SYSTEM ->
                listOf(
                    HomepageItem("employees", "Show all employees"),
                    HomepageItem("entertime", "Enter time"),
                    HomepageItem("timeentries", "Show all time entriest"),
                    HomepageItem("logging", "Log configuration"),
                    HomepageItem("logout", "Logout")
                )
        }
        return """
        <h2>
            Hello, <span id="username">${safeHtml(user.name.value)}</span>
        </h2>
        <nav>
            <ul>
                ${allowedToSee.joinToString("") { renderHomepageItem(it.link, it.descr) }}
            </ul>
        </nav>
        """
    }
    private fun renderHomepageItem(linkTarget: String, description: String): String {

        val item = """
        <li><a href="${safeAttr(linkTarget)}">${safeHtml(description)}</a></li>
        """
        return item
    }

    private val homepageHTML = """
<nav>
    <ul>
        <li><a href="login">Login</a></li>
        <li><a href="register">Register</a></li>
    </ul>
</nav>
"""

}