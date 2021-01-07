package coverosR3z.timerecording.api

import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.EmployeeName
import coverosR3z.authentication.types.UserName
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.utility.successHTML
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.utility.okHTML
import coverosR3z.timerecording.utility.ITimeRecordingUtilities

class EmployeeAPI {

    enum class Elements(val elemName: String, val id: String) {
        EMPLOYEE_INPUT("employee_name", "employee_name"),
        CREATE_BUTTON("", "employee_create_button"),
    }

    companion object {

        /**
         * The required inputs for this API
         */
        val requiredInputs = setOf(Elements.EMPLOYEE_INPUT.elemName)

        fun handlePOST(tru: ITimeRecordingUtilities, data: Map<String, String>) : PreparedResponseData {
            tru.createEmployee(EmployeeName.make(data[Elements.EMPLOYEE_INPUT.elemName]))
            return okHTML(successHTML)
        }

        fun generateCreateEmployeePage(username : UserName): String =
            createEmployeeHTML(username.value)

        fun generateExistingEmployeesPage(username : UserName, tru: ITimeRecordingUtilities): String =
            existingEmployeesHTML(username.value, tru.listAllEmployees())

        private fun createEmployeeHTML(username : String) : String {
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
                    <label for="${Elements.EMPLOYEE_INPUT.elemName}">Name:</label>
                    <input name="${Elements.EMPLOYEE_INPUT.elemName}" id="${Elements.EMPLOYEE_INPUT.id}" type="text" />
                </p>
            
                <p>
                    <button id="${Elements.CREATE_BUTTON.id}">Create new employee</button>
                </p>
            
            </form>
            </body>
        </html>        
        """
        }


        private fun existingEmployeesHTML(username : String, employees : List<Employee>) : String {
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

    }
}