package coverosR3z.authentication.api

import coverosR3z.authentication.types.Roles
import coverosR3z.server.types.GetEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.PageComponents

class LogoutAPI(private val sd: ServerData) {

    companion object : GetEndpoint {

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val l = LogoutAPI(sd)
            return doGETRequireAuth(sd.ahd.user, Roles.ADMIN, Roles.APPROVER, Roles.REGULAR) { l.generateLogoutPage() }
        }

        override val path: String
            get() = "logout"

    }

    fun generateLogoutPage(): String {
        sd.bc.au.logout(sd.ahd.user)
        return PageComponents.makeTemplate("You have logged out", "LogoutAPI", logoutHTML, extraHeaderContent="""<link rel="stylesheet" href="logoutpage.css" />""")
    }

    private val logoutHTML =
"""
<div class="container">
    <p>
        You are now logged out
    </p>

    <p><a href="homepage">Homepage</a></p>
</div>
"""
}