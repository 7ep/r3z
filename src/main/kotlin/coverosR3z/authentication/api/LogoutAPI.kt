package coverosR3z.authentication.api

import coverosR3z.server.types.GetEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth

class LogoutAPI(private val sd: ServerData) {

    companion object : GetEndpoint {

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val l = LogoutAPI(sd)
            return doGETRequireAuth(sd.authStatus) { l.generateLogoutPage() }
        }

        override val path: String
            get() = "logout"

    }

    fun generateLogoutPage(): String {
        sd.au.logout(sd.ahd.user)
        return logoutHTML
    }

    private val logoutHTML = """
<!DOCTYPE html>    
<html>
    <head>
        <title>Logout</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="stylesheet" href="general.css" />
    </head>
    <body>
        <div class="container">
            <p>
                You are now logged out
            </p>

            <p><a href="homepage">Homepage</a></p>
        </div>
    </body>
</html>    
"""
}