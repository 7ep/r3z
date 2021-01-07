package coverosR3z.server.api

import coverosR3z.server.types.Endpoint
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.doGETAuthAndUnauth

class HomepageAPI(private val sd: ServerData){

    companion object : Endpoint {

        override fun respond(sd: ServerData) : PreparedResponseData {
            val hp = HomepageAPI(sd)
            return doGETAuthAndUnauth(sd.authStatus, { hp.generateAuthHomepage() }, { hp.generateUnAuthenticatedHomepage() })
        }
    }


    fun generateAuthHomepage(): String = authHomePageHTML(sd.rd.user.name.value)
    fun generateUnAuthenticatedHomepage(): String = homepageHTML

    private fun authHomePageHTML(username: String): String {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <link rel="stylesheet" href="general.css" />
            <title>Authenticated Homepage</title>
            <meta name="viewport" content="width=device-width, initial-scale=1">
        </head>        
        <body>
            <div class="container">
                <h2>You are on the authenticated homepage, ${safeHtml(username)}</h2>
                <p><a href="createemployee">Create employee</a></p>
                <p><a href="employees">Show all employees</a></p>
                <p><a href="createproject">Create project</a></p>
                <p><a href="entertime">Enter time</a></p>
                <p><a href="timeentries">Show all time entries</a></p>
                <p><a href="logging">Log configuration</a></p>
                <p><a href="logout">Logout</a></p>
            </div>
        </body>
    </html>
"""
    }

    private val homepageHTML = """
<!DOCTYPE html>    
<html lang="en">
    <head>
        <link rel="stylesheet" href="general.css" />
        <title>Homepage</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    </head>
    <header><a class="home-button" href="homepage">r3z</a></header>
    <body>
        <div class="container">
            <h2>You are on the homepage</h2>
            <p><a href="login">Login</a></p>
            <p><a href="register">Register</a></p>
        </div>
    </body>
</html>
"""

}