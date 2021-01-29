package coverosR3z.server.api

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
            return doGETAuthAndUnauth(sd.authStatus, { hp.authHomePageHTML() }, { hp.homepageHTML })
        }

        override val path: String
            get() = "homepage"
    }

    private fun authHomePageHTML(): String {
        val username = safeHtml(sd.ahd.user.name.value)
        val body = """
            <div class="container">
                <h2>You are on the authenticated homepage, $username</h2>
                <p><a href="createemployee">Create employee</a></p>
                <p><a href="employees">Show all employees</a></p>
                <p><a href="createproject">Create project</a></p>
                <p><a href="entertime">Enter time</a></p>
                <p><a href="timeentries">Show all time entries</a></p>
                <p><a href="logging">Log configuration</a></p>
                <p><a href="logout">Logout</a></p>
            </div>
"""
        return PageComponents.makeTemplate("Authenticated Homepage", "HomepageAPI", body)
    }

    private val homepageHTML = """
<!DOCTYPE html>    
<html lang="en">
    <head>
        <link rel="stylesheet" href="general.css" />
        <title>Homepage</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta apifile="HomepageAPI" >
    </head>
    <body>
        ${PageComponents.standardHeader}
        <div class="container">
            <h2>You are on the homepage</h2>
            <p><a href="login">Login</a></p>
            <p><a href="register">Register</a></p>
        </div>
    </body>
</html>
"""

}