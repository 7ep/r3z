package coverosR3z.webcontent

import coverosR3z.domainobjects.Employee

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
            employees.joinToString("") { "<tr><td>${it.id}</td><td>${it.name}</td></tr>\n" } +
"""
            </tbody>
        </table>

    </body>
</html>
        """
}