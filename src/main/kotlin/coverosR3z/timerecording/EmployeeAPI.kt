package coverosR3z.timerecording

import coverosR3z.domainobjects.Employee
import coverosR3z.domainobjects.EmployeeName
import coverosR3z.domainobjects.User
import coverosR3z.misc.checkHasExactInputs
import coverosR3z.misc.safeHtml
import coverosR3z.server.*
import coverosR3z.misc.successHTML

enum class EmployeeElements(val elemName: String, val id: String) {
    EMPLOYEE_INPUT("employee_name", "employee_name"),
    CREATE_BUTTON("", "employee_create_button");

}
/**
 * The required inputs for this API
 */
val requiredElements = setOf(EmployeeElements.EMPLOYEE_INPUT.elemName)

fun handlePOSTNewEmployee(tru: ITimeRecordingUtilities, user: User, data: Map<String, String>) : PreparedResponseData {
    return if (isAuthenticated(user)) {
        checkHasExactInputs(data.keys, requiredElements)
        tru.createEmployee(EmployeeName.make(data[EmployeeElements.EMPLOYEE_INPUT.elemName]))
        okHTML(successHTML)
    } else {
        handleUnauthorized()
    }
}

fun doGETCreateEmployeePage(rd: AnalyzedHttpData): PreparedResponseData {
    return if (isAuthenticated(rd)) {
        okHTML(createEmployeeHTML(rd.user.name.value))
    } else {
        redirectTo(NamedPaths.HOMEPAGE.path)
    }
}


fun createEmployeeHTML(username : String) : String {
    return """
<!DOCTYPE html>        
<html>
    <head>
        <title>create employee</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    </head>
    <body>
    <form action="createemployee" method="post">
    
        <p>
            Hello there, <span id="username">${safeHtml(username)}</span>!
        </p>
    
        <p>
            <label for="${EmployeeElements.EMPLOYEE_INPUT.elemName}">Name:</label>
            <input name="${EmployeeElements.EMPLOYEE_INPUT.elemName}" id="${EmployeeElements.EMPLOYEE_INPUT.id}" type="text" />
        </p>
    
        <p>
            <button id="${EmployeeElements.CREATE_BUTTON.id}">Create new employee</button>
        </p>
    
    </form>
    </body>
</html>        
"""
}


fun existingEmployeesHTML(username : String, employees : List<Employee>) : String {
    return """
<!DOCTYPE html>        
<html>
    <head>
        <title>Company Employees</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
        table,
        td {
            border: 1px solid #333;
        }

        thead,
        tfoot {
            background-color: #333;
            color: #fff;
        }

        </style>
    </head>
    <body>
        <p>
            Here are the employees at your company, <span id="username">${safeHtml(username)}</span>
        </p>
        <table>
            <thead>
                <tr>
                    <th>Identifier</th>
                    <th>Name</th>
                </tr>
            </thead>
            <tbody>
            """ +employees.joinToString("") { "<tr><td>${it.id.value}</td><td>${safeHtml(it.name.value)}</td></tr>\n" } + """
            </tbody>
        </table>

    </body>
</html>
"""
}