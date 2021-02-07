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
        <a href="createemployee">Create employee</a>
        <a href="employees">Show all employees</a>
        <a href="createproject">Create project</a>
        <a href="entertime">Enter time</a>
        <a href="timeentries">Show all time entries</a>
        <a href="logging">Log configuration</a>
        <a href="logout">Logout</a>
</nav>
"""
        return PageComponents.makeTemplate("Authenticated Homepage", "HomepageAPI", body, extraHeaderContent="""<link rel="stylesheet" href="authhomepage.css" />""")
    }

    private val homepageHTML = """
        <nav>
            <a href="login">Login</a>
            <a href="register">Register</a>
        </nav>
"""

}