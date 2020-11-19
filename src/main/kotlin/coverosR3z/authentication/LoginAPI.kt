package coverosR3z.authentication

import coverosR3z.domainobjects.*
import coverosR3z.logging.logInfo
import coverosR3z.server.*
import coverosR3z.webcontent.successHTML

fun handlePOSTLogin(au: IAuthenticationUtilities, user: User, data: Map<String, String>) : PreparedResponseData {
    val isUnauthenticated = user == NO_USER
    return if (isUnauthenticated) {
        val username = UserName.make(data["username"])
        val password = Password.make(data["password"])
        val (loginResult, loginUser) = au.login(username, password)
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
<!DOCTYPE html>
<html>
  <head>
      <link rel="stylesheet" href="login.css">
      <link href="https://fonts.googleapis.com/css?family=Ubuntu" rel="stylesheet">
      <title>register</title>
       <link rel="stylesheet" href="login.css" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
  </head>
  <header><a href="homepage">r3z</a></header>
  <body>
    <br>
    <h2>Login</h2>
    
    <form  method="post" action="login">
      <div class="container"> 
        <label>Username</label>
        <input class="input" type="text" name="username" id="username">
        <label>Password</label>
        <input class="input" type="password" name="password" id="password">
        <button id="login_button" class="submit">Login</button>
      </div>
    </form>
  </body>
</html>      
"""


const val loginCSS = """
    header {
      color: #ffffff;
      background-color: #809EE4;
      font-size: 30px;
      margin: 0;
      top: 0;
      height: 40px;
      padding: 4px;
    }
    
    body {
      color: #333333;
      background-color: #F3F6FA;
      font-family: "helvetica", sans-serif;
      padding-top: 40px;
      margin: 0;
      padding: 0;
      display: flex;
      flex-flow: column wrap;
    }
    
    h2 {
      text-align: center;
      font-size: 160%;
      margin: 10px;
    }
    a {
      color: #ffffff;
      text-decoration: none;
    }
    a:visited {
      color: #ffffff;
    }
    
    form {
      font-size: 90%;
      border: 2px solid #858585;
      padding: 10px;
      border-radius: .5em;
      width: 90%;
      max-width: 350px;
      margin: auto;
    }
    
    input {
      height: 1.7em;
      margin-bottom: 1em;
    }
    
    button {
      cursor: pointer;
      font-size: 120%;;
      margin: 8px 0 1px 0;
      padding: 2%;
      color: #ffff;
      background-color: #5E64E3;
      border: none;
      border-radius: .5em;
      width: 100%;
      position: center;
    }
    button:hover {
      color: #5f5f5f;
      background-color: #DDDDE5;
    }
    
    .container {
      line-height: 1.5;
      margin: .2em 0 .2em 0;
      text-align: left;
    }
    
    .input {
      border: 1.5px solid #6b6969;
      border-radius: .3em;
      width: 100%;
      background-color: inherit;
      color: #5f5f5f;
    }
"""