package coverosR3z.webcontent

import coverosR3z.domainobjects.TimeEntry

fun existingTimeEntriesHTML(username : String, te : List<TimeEntry>) : String {
    return """
<html>
    <head>
        <title>your time entries</title>
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
            Here are your entries, <span id="username">$username</span>
        </p>
        <table>
            <thead>
                <tr>
                    <th>Project</th>
                    <th>Time</th>
                    <th>Details</th>
                    <th>Date</th>
                </tr>
            </thead>
            <tbody>
                
""" +
            te.joinToString("") { "<tr><td>${it.project.name}</td><td>${it.time.numberOfMinutes}</td><td>${it.details.value}</td><td>${it.date.stringValue}</td></tr>\n" } +
"""    
            </tbody>
        </table>

    </body>
</html>
"""
}