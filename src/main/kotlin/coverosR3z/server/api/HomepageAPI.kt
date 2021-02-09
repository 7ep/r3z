package coverosR3z.server.api

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
        val body =
"""
<nav>
    <ul>
        <li><a href="createemployee">Create employee</a></li>
        <li><a href="employees">Show all employees</a></li>
        <li><a href="createproject">Create project</a></li>
        <li><a href="entertime">Enter time</a></li>
        <li><a href="timeentries">Show all time entries</a></li>
        <li><a href="logging">Log configuration</a></li>
        <li><a href="logout">Logout</a></li>
    </ul>
</nav>
"""
        return PageComponents.makeTemplate("Authenticated Homepage", "HomepageAPI", body, extraHeaderContent="""<link rel="stylesheet" href="authhomepage.css" />""")
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