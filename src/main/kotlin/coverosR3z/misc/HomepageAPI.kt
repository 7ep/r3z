package coverosR3z.misc

import coverosR3z.server.PreparedResponseData
import coverosR3z.server.RequestData
import coverosR3z.server.isAuthenticated
import coverosR3z.server.okHTML


fun doGetHomePage(rd: RequestData): PreparedResponseData {
    return if (isAuthenticated(rd)) {
        okHTML(authHomePageHTML(rd.user.name.value))
    } else {
        okHTML(homepageHTML)
    }
}

fun authHomePageHTML(username : String) : String {
    return """
        <!DOCTYPE html>
        <html>
    <head>
    </head>
        <title>Authenticated Homepage</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
      <h2>You are on the authenticated homepage, $username</h2>
       <p><a href="createemployee">Create employee</a></p>
       <p><a href="employees">Show all employees</a></p>
       <p><a href="createproject">Create project</a></p>
       <p><a href="entertime">Enter time</a></p>
       <p><a href="timeentries">Show all time entries</a></p>
       <p><a href="logout">Logout</a></p>
    </body>
</html>
"""
}

const val homepageHTML = """
<!DOCTYPE html>    
<html>
    <head>
      <link rel="stylesheet" href="general.css" />
    </head>
        <title>Homepage</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
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