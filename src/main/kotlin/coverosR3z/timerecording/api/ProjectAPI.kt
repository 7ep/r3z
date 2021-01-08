package coverosR3z.timerecording.api

import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.GetEndpoint
import coverosR3z.server.types.PostEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.doGETRequireAuth
import coverosR3z.server.utility.doPOSTAuthenticated
import coverosR3z.server.utility.okHTML
import coverosR3z.server.utility.successHTML
import coverosR3z.timerecording.types.ProjectName

class ProjectAPI(private val sd: ServerData) {

    enum class Elements(val elemName: String, val id: String) {
        PROJECT_INPUT("project_name", "project_name"),
        CREATE_BUTTON("", "project_create_button"),
    }

    companion object : GetEndpoint, PostEndpoint {

        override val requiredInputs = setOf(
            Elements.PROJECT_INPUT.elemName
        )
        override val path: String
            get() = "createproject"

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val p = ProjectAPI(sd)
            return doGETRequireAuth(sd.authStatus) { p.createProjectHTML() }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val p = ProjectAPI(sd)
            return doPOSTAuthenticated(sd.authStatus, requiredInputs, sd.ahd.data) { p.handlePOST() }
        }


    }

    fun handlePOST() : PreparedResponseData {
        sd.tru.createProject(ProjectName.make(sd.ahd.data[Elements.PROJECT_INPUT.elemName]))
        return okHTML(successHTML)
    }

    private fun createProjectHTML() : String {
        val username = safeHtml(sd.ahd.user.name.value)

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
                <label for="${Elements.PROJECT_INPUT.elemName}">Name:</label>
                <input name="${Elements.PROJECT_INPUT.elemName}" id="${Elements.PROJECT_INPUT.id}" type="text" />
            </p>
        
            <p>
                <button id="${Elements.CREATE_BUTTON.id}">Create new project</button>
            </p>
        
        </form>
    </body>
</html>
"""
    }
}

