package coverosR3z.timerecording.api

import coverosR3z.authentication.types.User
import coverosR3z.authentication.types.UserName
import coverosR3z.domainobjects.*
import coverosR3z.misc.safeHtml
import coverosR3z.misc.successHTML
import coverosR3z.misc.types.Date
import coverosR3z.server.*
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.timerecording.utility.ITimeRecordingUtilities

class EnterTimeAPI {

    enum class Elements (val elemName: String, val id: String) {
        PROJECT_INPUT("project_entry", "project_entry"),
        TIME_INPUT("time_entry", "time_entry"),
        DETAIL_INPUT("detail_entry", "detail_entry"),
        ENTER_TIME_BUTTON("", "enter_time_button"),
        DATE_INPUT("date_entry", "date_entry"),
    }

    companion object {



        val requiredInputs = setOf(
            Elements.PROJECT_INPUT.elemName,
            Elements.TIME_INPUT.elemName,
            Elements.DETAIL_INPUT.elemName,
            Elements.DATE_INPUT.elemName,
        )

        fun generateTimeEntriesPage(tru: ITimeRecordingUtilities, user : User): String =
            existingTimeEntriesHTML(user.name.value, tru.getAllEntriesForEmployee(user.employeeId ?: NO_EMPLOYEE.id))

        fun generateEnterTimePage(tru : ITimeRecordingUtilities, username : UserName): String =
            entertimeHTML(username.value, tru.listAllProjects())


        fun handlePOST(tru: ITimeRecordingUtilities, employeeId : EmployeeId?, data: Map<String, String>) : PreparedResponseData {
                val projectId = ProjectId.make(data[Elements.PROJECT_INPUT.elemName])
                val time = Time.make(data[Elements.TIME_INPUT.elemName])
                val details = Details.make(data[Elements.DETAIL_INPUT.elemName])
                val date = Date.make(data[Elements.DATE_INPUT.elemName])

                val project = tru.findProjectById(projectId)
                val employee = tru.findEmployeeById(checkNotNull(employeeId){employeeIdNotNullMsg})

                val timeEntry = TimeEntryPreDatabase(
                        employee,
                        project,
                        time,
                        date,
                        details)

                tru.recordTime(timeEntry)

                return okHTML(successHTML)
        }


        private fun entertimeHTML(username: String, projects : List<Project>) : String {
            return """
            <!DOCTYPE html>        
            <html>
            <head>
                <title>enter time</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <link rel="stylesheet" href="entertime.css" />
                <script src="entertime.js"></script>
            </head>
            <body>
                <form action="entertime" method="post">
        
                    <p>
                        Hello there, <span id="username">${safeHtml(username)}</span>!
                    </p>
        
                    <p>
                        <label for="project_entry">Project:</label>
                        <select name="project_entry" id="project_entry"/>
                        """ + projects.joinToString("") { "<option value =\"${it.id.value}\">${safeHtml(it.name.value)}</option>\n" } +"""             <option selected disabled hidden>Choose here</option>
                    </select>
                    </p>
        
                    <p>
                        <label for="${Elements.TIME_INPUT.elemName}">Time:</label>
                        <input name="${Elements.TIME_INPUT.elemName}" id="${Elements.TIME_INPUT.id}" type="text" />
                    </p>
        
                    <p>
                        <label for="${Elements.DETAIL_INPUT.elemName}">Details:</label>
                        <input name="${Elements.DETAIL_INPUT.elemName}" id="${Elements.DETAIL_INPUT.id}" type="text" />
                    </p>
                    
                    <p>
                        <label for="${Elements.DATE_INPUT.elemName}">Date:</label>
                        <input name="${Elements.DATE_INPUT.elemName}" id="${Elements.DATE_INPUT.id}" type="date" />
                    </p>
                    
                    <p>
                        <button id="${Elements.ENTER_TIME_BUTTON.id}">Enter time</button>
                    </p>
        
                </form>
            </body>
        </html>
        """
        }


        private fun existingTimeEntriesHTML(username : String, te : Set<TimeEntry>) : String {
            return """
        <!DOCTYPE html>        
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
                        
                    """ + te.joinToString("") { "<tr id=time-entry-${it.employee.id.value}-${it.id}><td>${safeHtml(it.project.name.value)}</td><td class='time'><input type=text value=${it.time.numberOfMinutes}></input></td><td>${safeHtml(it.details.value)}</td><td>${it.date.stringValue}</td></tr>\n" } + """    
                    </tbody>
                </table>
        
            </body>
        </html>
        """
        }


    }
}