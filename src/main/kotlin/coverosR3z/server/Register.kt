package coverosR3z.server

import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.domainobjects.NO_USER
import coverosR3z.domainobjects.RegistrationResult
import coverosR3z.domainobjects.User
import coverosR3z.misc.checkParseToInt
import coverosR3z.timerecording.ITimeRecordingUtilities
import coverosR3z.webcontent.failureHTML
import coverosR3z.webcontent.registerHTML
import coverosR3z.webcontent.successHTML


fun doGETRegisterPage(tru: ITimeRecordingUtilities, rd: RequestData): PreparedResponseData {
    return if (ServerUtilities.isAuthenticated(rd)) {
        ServerUtilities.redirectTo(NamedPaths.AUTHHOMEPAGE.path)
    } else {
        val employees = tru.listAllEmployees()
        PreparedResponseData(registerHTML(employees), ResponseStatus.OK)
    }
}

fun handlePOSTRegister(au: IAuthenticationUtilities, user: User, data: Map<String, String>) : PreparedResponseData {
    return if (user == NO_USER) {
        val username = checkNotNull(data["username"]) {"username must not be missing"}
        check(username.isNotBlank()) {"The username must not be blank"}
        val password = checkNotNull(data["password"])  {"password must not be missing"}
        check(password.isNotBlank()) {"The password must not be blank"}
        val employeeId = checkNotNull(data["employee"])  {"employee must not be missing"}
        check(employeeId.isNotBlank()) {"The employee must not be blank"}
        val employeeIdInt = checkParseToInt(employeeId){"Must be able to convert $employeeId to an int"}
        check(employeeIdInt > 0) {"The employee id must be greater than zero"}
        val result = au.register(username, password, employeeIdInt)
        if (result == RegistrationResult.SUCCESS) {
            ServerUtilities.okHTML(successHTML)
        } else {
            PreparedResponseData(failureHTML, ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct))
        }
    } else {
        ServerUtilities.redirectTo(NamedPaths.AUTHHOMEPAGE.path)
    }
}