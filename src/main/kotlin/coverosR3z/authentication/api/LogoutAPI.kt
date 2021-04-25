package coverosR3z.authentication.api

import coverosR3z.authentication.types.Role
import coverosR3z.server.types.GetEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth

class LogoutAPI(private val sd: ServerData) {

    companion object : GetEndpoint {

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val l = LogoutAPI(sd)
            return doGETRequireAuth(sd.ahd.user, Role.ADMIN, Role.APPROVER, Role.REGULAR) { l.generateLogoutPage() }
        }

        override val path: String
            get() = "logout"

    }

    fun generateLogoutPage(): String {
        sd.bc.au.logout(sd.ahd.user)
        return """
            <!DOCTYPE html>    
<html lang="en">
    <head>
        <link rel="stylesheet" href="general.css" />
        <link rel="stylesheet" href="logoutpage.css" />
        <title>You have logged out</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="file" content="LogoutAPI" >
    </head>
    <body>
        <div class="container">
            <p>
                You are now logged out
            </p>
        
            <a class="button" href="homepage">OK</a>
        </div>
    </body>
</html>
"""

    }

}