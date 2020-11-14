package coverosR3z.webcontent

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