package coverosR3z.timerecording

import coverosR3z.domainobjects.ProjectName
import coverosR3z.domainobjects.User
import coverosR3z.domainobjects.UserName
import coverosR3z.misc.checkHasExactInputs
import coverosR3z.misc.safeHtml
import coverosR3z.misc.successHTML
import coverosR3z.server.*

enum class ProjectElements(val elemName: String, val id: String) {
    PROJECT_INPUT("project_name", "project_name"),
    CREATE_BUTTON("", "project_create_button"),
}

private val requiredInputs = setOf(
    ProjectElements.PROJECT_INPUT.elemName
)

fun handlePOSTCreatingProject(tru: ITimeRecordingUtilities, user: User, data: Map<String, String>) : PreparedResponseData {
    val isAuthenticated = isAuthenticated(user)
    return if (isAuthenticated) {
        checkHasExactInputs(data.keys, requiredInputs)
        tru.createProject(ProjectName.make(data[ProjectElements.PROJECT_INPUT.elemName]))
        okHTML(successHTML)
    } else {
        handleUnauthorized()
    }
}


fun generateCreateProjectPage(username: UserName): String = createProjectHTML(username.value)


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
                Hello there, <span id="username">${safeHtml(username)}</span>!
            </p>
        
            <p>
                <label for="${ProjectElements.PROJECT_INPUT.elemName}">Name:</label>
                <input name="${ProjectElements.PROJECT_INPUT.elemName}" id="${ProjectElements.PROJECT_INPUT.id}" type="text" />
            </p>
        
            <p>
                <button id="${ProjectElements.CREATE_BUTTON.id}">Create new project</button>
            </p>
        
        </form>
    </body>
</html>
"""
}