package coverosR3z.webcontent

fun createProjectHTML(username : String) : String {
return """
<html>
    <head>
        <title>create project</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    </head>
    <body>
        <form action="createproject" method="post">
        
            <p>
                Hello there, <span id="username">$username</span>!
            </p>
        
            <p>
                <label for="project_name">Name:</label>
                <input name="project_name" id="project_name" type="text" />
            </p>
        
            <p>
                <button id="project_create_button">Create new project</button>
            </p>
        
        </form>
    </body>
</html>
"""
}