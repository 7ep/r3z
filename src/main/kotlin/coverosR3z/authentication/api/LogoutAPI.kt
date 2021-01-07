package coverosR3z.authentication.api

import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.IAuthenticationUtilities

fun generateLogoutPage(au: IAuthenticationUtilities, user : User): String {
    au.logout(user)
    return logoutHTML
}

const val logoutHTML = """
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