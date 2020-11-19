package coverosR3z.timerecording

import coverosR3z.domainobjects.NO_USER
import coverosR3z.domainobjects.ProjectName
import coverosR3z.domainobjects.User
import coverosR3z.server.*
import coverosR3z.webcontent.successHTML

fun handlePOSTCreatingProject(tru: ITimeRecordingUtilities, user: User, data: Map<String, String>) : PreparedResponseData {
    val isAuthenticated = user != NO_USER
    return if (isAuthenticated) {
        tru.createProject(ProjectName.make(data["project_name"]))
        PreparedResponseData(successHTML, ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct))
    } else {
        handleUnauthorized()
    }
}


fun doGETCreateProjectPage(rd: RequestData): PreparedResponseData {
    return if (isAuthenticated(rd)) {
        okHTML(createProjectHTML(rd.user.name.value))
    } else {
        redirectTo(NamedPaths.HOMEPAGE.path)
    }
}


fun createProjectHTML(username : String) : String {
    return """
<!DOCTYPE html>        
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