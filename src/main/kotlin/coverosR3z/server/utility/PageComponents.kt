package coverosR3z.server.utility

import coverosR3z.authentication.api.ChangePasswordAPI
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.Role
import coverosR3z.system.misc.utility.safeHtml
import coverosR3z.server.types.ServerData

class PageComponents(sd: ServerData) {

        val user = sd.ahd.user
        private val includeHomepageLinkIfAdmin =  if (user.role == Role.ADMIN) """<a class="header-button" href="homepage">homepage</a>""" else ""
        private val standardHeader =
"""
<header>
        <span>$includeHomepageLinkIfAdmin</span>${renderUserInfo()}
</header>
""".trimIndent()

        private fun renderUserInfo(): String {
                return if (user == NO_USER) "" else
"""<span id="right-side-header">
        <span id="username">
                <a class="header-button" href="${ChangePasswordAPI.path}">${safeHtml(user.name.value)}</a>
        </span> 
        <a id="header-logout" class="header-button" href="logout">logout</a>
</span>""".trimMargin()
        }

        fun makeTemplate(title: String, apiFile: String, body: String, extraHeaderContent: String="") = """
<!DOCTYPE html>    
<html lang="en">
    <head>
        <link rel="stylesheet" href="general.css" />
        $extraHeaderContent
        <title>$title</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="file" content="$apiFile" >
    </head>
    <body>
        $standardHeader
        $body
    </body>
</html>
"""

}