package coverosR3z.webcontent

fun authHomePageHTML(username : String) : String {
    return """
        <html>
    <head>
    </head>
        <title>Authenticated Homepage</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
      <h2>You are on the authenticated homepage, $username</h2>
       <p><a href="createemployee">Create employee</a></p>
       <p><a href="employees">Show all employees</a></p>
       <p><a href="createproject">Create project</a></p>
       <p><a href="entertime">Enter time</a></p>
       <p><a href="timeentries">Show all time entries</a></p>
       <p><a href="logout">Logout</a></p>
    </body>
</html>
"""
}