package coverosR3z.authentication

import coverosR3z.domainobjects.*
import coverosR3z.misc.checkParseToInt
import coverosR3z.server.*
import coverosR3z.timerecording.ITimeRecordingUtilities
import coverosR3z.webcontent.failureHTML
import coverosR3z.webcontent.successHTML


fun doGETRegisterPage(tru: ITimeRecordingUtilities, rd: RequestData): PreparedResponseData {
    return if (isAuthenticated(rd)) {
        redirectTo(NamedPaths.AUTHHOMEPAGE.path)
    } else {
        val employees = tru.listAllEmployees()
        PreparedResponseData(registerHTML(employees), ResponseStatus.OK)
    }
}

fun handlePOSTRegister(au: IAuthenticationUtilities, user: User, data: Map<String, String>) : PreparedResponseData {
    return if (user == NO_USER) {
        val username = UserName(checkNotNull(data["username"]) {"username must not be missing"})
        val password = checkNotNull(data["password"])  {"password must not be missing"}
        check(password.isNotBlank()) {"The password must not be blank"}
        val employeeId = checkNotNull(data["employee"])  {"employee must not be missing"}
        check(employeeId.isNotBlank()) {"The employee must not be blank"}
        val employeeIdInt = checkParseToInt(employeeId)
        check(employeeIdInt > 0) {"The employee id must be greater than zero"}
        val result = au.register(username.value, password, employeeIdInt)
        if (result == RegistrationResult.SUCCESS) {
            okHTML(successHTML)
        } else {
            PreparedResponseData(failureHTML, ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct))
        }
    } else {
        redirectTo(NamedPaths.AUTHHOMEPAGE.path)
    }
}

fun registerHTML(employees: List<Employee>) : String {
    return """
<html>
  <head>
      <link rel="stylesheet" href="register.css">
      <link href="https://fonts.googleapis.com/css?family=Ubuntu" rel="stylesheet">
      <title>register</title>
      <link rel="stylesheet" href="entertime.css" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
  </head>
  <header>r3z</header>
  <body>
    <br>
    <h2>Register a User</h2>
    
    <form  method="post" action="register">
      <div class="container"> 
        <label>Username</label>
        <input class="input" type="text" name="username" id="username">
        <label>Password</label>
        <input class="input" type="password" name="password" id="password">
        <label>Employee</label>
        <select class="input" id="employee" name="employee">
"""+employees.joinToString("") { "<option value =\"${it.id.value}\">${it.name.value}</option>\n" } +
            """
          <option value ="1">Administrator</option>
          <option selected disabled hidden>Choose here</option>
        </select>
        <button id="register_button" class="submit">Register</button>
      </div>
    </form>
  </body>
</html
"""
}

const val registerCSS = """
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