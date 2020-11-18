package coverosR3z.authentication

import coverosR3z.domainobjects.LoginResult
import coverosR3z.domainobjects.NO_USER
import coverosR3z.domainobjects.User
import coverosR3z.domainobjects.UserName
import coverosR3z.logging.logInfo
import coverosR3z.server.*
import coverosR3z.webcontent.successHTML

fun handlePOSTLogin(au: IAuthenticationUtilities, user: User, data: Map<String, String>) : PreparedResponseData {
    val isUnauthenticated = user == NO_USER
    return if (isUnauthenticated) {
        val username = UserName(checkNotNull(data["username"]) {"username must not be missing"})
        val password = checkNotNull(data["password"]) {"password must not be missing"}
        check(password.isNotBlank()) {"The password must not be blank"}
        val (loginResult, loginUser) = au.login(username.value, password)
        if (loginResult == LoginResult.SUCCESS && loginUser != NO_USER) {
            val newSessionToken: String = au.createNewSession(loginUser)
            PreparedResponseData(successHTML, ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct, "Set-Cookie: sessionId=$newSessionToken"))
        } else {
            logInfo("User ($username) failed to login")
            handleUnauthorized()
        }
    } else {
        redirectTo(NamedPaths.AUTHHOMEPAGE.path)
    }
}

fun doGETLoginPage(rd: RequestData): PreparedResponseData {
    return if (isAuthenticated(rd)) {
        redirectTo(NamedPaths.AUTHHOMEPAGE.path)
    } else {
        okHTML(loginHTML)
    }
}

const val loginHTML = """
<html>
    <head>
        <title>login</title>
    </head>
    <body>
            <h2>Login</h2>
            <form action="login" method="post">

                <label for="username">Username</label>
                <input type="text" name="username" id="username" />

                <label for="password">Password</label>
                <input type="password" name="password" id="password" />

                <button id="login_button">Login</button>
            </form>
    </body>
</html>        
"""