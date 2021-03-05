package coverosR3z.server.utility

import coverosR3z.authentication.types.NO_USER
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.ServerData

class PageComponents(private val sd: ServerData) {

        val user = sd.ahd.user
        private val standardHeader =
"""
<header><span><a class="header-button" href="homepage">homepage</a></span>${renderUserInfo()}</header>
""".trimIndent()

        private fun renderUserInfo(): String {
                return when(user) {
                        NO_USER ->
                                """<span><a class="header-button" href="login">login</a></span>"""
                        else ->
                                """<span><span id="username">${safeHtml(user.name.value)}</span> <a class="header-button" href="logout">logout</a></span>"""
                }

        }

        fun makeTemplate(title: String, apiFile: String, body: String, extraHeaderContent: String="") = """
<!DOCTYPE html>    
<html lang="en">
    <head>
        <link rel="stylesheet" href="general.css" />
        $extraHeaderContent
        <title>$title</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta apifile="$apiFile" >
    </head>
    <body>
        $standardHeader
        $body
    </body>
</html>
"""

}