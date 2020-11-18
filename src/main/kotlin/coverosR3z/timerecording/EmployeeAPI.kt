package coverosR3z.timerecording

import coverosR3z.domainobjects.Employee
import coverosR3z.domainobjects.EmployeeName
import coverosR3z.domainobjects.User
import coverosR3z.server.*
import coverosR3z.webcontent.successHTML

fun handlePOSTNewEmployee(tru: ITimeRecordingUtilities, user: User, data: Map<String, String>) : PreparedResponseData {
    return if (isAuthenticated(user)) {
        tru.createEmployee(EmployeeName(checkNotNull(data["employee_name"]){"The employee_name must not be missing"}))
        PreparedResponseData(successHTML, ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct))
    } else {
        handleUnauthorized()
    }
}

fun doGETCreateEmployeePage(rd: RequestData): PreparedResponseData {
    return if (isAuthenticated(rd)) {
        okHTML(createEmployeeHTML(rd.user.name.value))
    } else {
        redirectTo(NamedPaths.HOMEPAGE.path)
    }
}


fun createEmployeeHTML(username : String) : String {
    return """
<html>
    <head>
        <title>create employee</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    </head>
    <body>
    <form action="createemployee" method="post">
    
        <p>
            Hello there, <span id="username">$username</span>!
        </p>
    
        <p>
            <label for="employee_name">Name:</label>
            <input name="employee_name" id="employee_name" type="text" />
        </p>
    
        <p>
            <button id="employee_create_button">Create new employee</button>
        </p>
    
    </form>
    </body>
</html>        
"""
}


fun existingEmployeesHTML(username : String, employees : List<Employee>) : String {
    return """
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
            Here are the employees at your company, <span id="username">$username</span>
        </p>
        <table>
            <thead>
                <tr>
                    <th>Identifier</th>
                    <th>Name</th>
                </tr>
            </thead>
            <tbody>
                
""" +
            employees.joinToString("") { "<tr><td>${it.id.value}</td><td>${it.name.value}</td></tr>\n" } +
            """
            </tbody>
        </table>

    </body>
</html>
        """
}