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
    </head>
    <body>
    <body>
      <div class="main">
        <p class="sign" align="center">Register</p>
        <form class="form1" method="post" action="register">
          <input class="un" type="text" align="center" name="username" id="username" placeholder="Username">
          <input class="pass" type="password" align="center" name="password" id="password" placeholder="Password">
           <select class="un" id="employee" name="employee">
"""+employees.joinToString("") { "<option value =\"${it.id.value}\">${it.name.value}</option>\n" } +
            """
            <option selected disabled hidden>Choose here</option>
          </select>
          <button id="register_button" class="submit" align="center">Register</button>
          </form>
        </div>
    </body>
</html>
"""
}

const val registerCSS = """
     body {
        background-color: #F3EBF6;
        font-family: 'Ubuntu', sans-serif;
    }
    
    .main {
        background-color: #FFFFFF;
        width: 400px;
        height: 400px;
        margin: 7em auto;
        border-radius: 1.5em;
        box-shadow: 0px 11px 35px 2px rgba(0, 0, 0, 0.14);
    }
    
    .sign {
        padding-top: 40px;
        color: #8C55AA;
        font-family: 'Ubuntu', sans-serif;
        font-weight: bold;
        font-size: 23px;
    }
    
    .un {
    width: 76%;
    color: rgb(38, 50, 56);
    font-weight: 700;
    font-size: 14px;
    letter-spacing: 1px;
    background: rgba(136, 126, 126, 0.04);
    padding: 10px 20px;
    border: none;
    border-radius: 20px;
    outline: none;
    box-sizing: border-box;
    border: 2px solid rgba(0, 0, 0, 0.02);
    margin-bottom: 50px;
    margin-left: 46px;
    text-align: center;
    margin-bottom: 27px;
    font-family: 'Ubuntu', sans-serif;
    }
    
    form.form1 {
        padding-top: 40px;
    }
    
    .pass {
            width: 76%;
    color: rgb(38, 50, 56);
    font-weight: 700;
    font-size: 14px;
    letter-spacing: 1px;
    background: rgba(136, 126, 126, 0.04);
    padding: 10px 20px;
    border: none;
    border-radius: 20px;
    outline: none;
    box-sizing: border-box;
    border: 2px solid rgba(0, 0, 0, 0.02);
    margin-bottom: 50px;
    margin-left: 46px;
    text-align: center;
    margin-bottom: 27px;
    font-family: 'Ubuntu', sans-serif;
    }
    
   
    .un:focus, .pass:focus {
        border: 2px solid rgba(0, 0, 0, 0.18) !important;
        
    }
    
    .submit {
      cursor: pointer;
        border-radius: 5em;
        color: #fff;
        background: linear-gradient(to right, #9C27B0, #E040FB);
        border: 0;
        padding-left: 40px;
        padding-right: 40px;
        padding-bottom: 10px;
        padding-top: 10px;
        font-family: 'Ubuntu', sans-serif;
        margin-left: 35%;
        font-size: 13px;
        box-shadow: 0 0 20px 1px rgba(0, 0, 0, 0.04);
    }
    
    .forgot {
        text-shadow: 0px 0px 3px rgba(117, 117, 117, 0.12);
        color: #E1BEE7;
        padding-top: 15px;
    }
    
    a {
        text-shadow: 0px 0px 3px rgba(117, 117, 117, 0.12);
        color: #E1BEE7;
        text-decoration: none
    }
    
    @media (max-width: 600px) {
        .main {
            border-radius: 0px;
        }
"""