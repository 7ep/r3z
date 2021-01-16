package coverosR3z.timerecording.api

import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.successHTML
import coverosR3z.timerecording.types.ProjectName

class ProjectAPI(private val sd: ServerData) {

    enum class Elements(private val elemName: String, private val id: String) : Element {
        PROJECT_INPUT("project_name", "project_name"),
        CREATE_BUTTON("", "project_create_button"),;

        override fun getId(): String {
            return this.id
        }

        override fun getElemName(): String {
            return this.elemName
        }

        override fun getElemClass(): String {
            throw NotImplementedError()
        }
    }

    companion object : GetEndpoint, PostEndpoint {

        override val requiredInputs = setOf(
            Elements.PROJECT_INPUT
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
        sd.tru.createProject(ProjectName.make(sd.ahd.data.mapping[Elements.PROJECT_INPUT.getElemName()]))
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
        <meta apifile="ProjectAPI" >
    </head>
    <body>
        <form action="$path" method="post">
        
            <p>
                Hello there, <span id="username">$username</span>!
            </p>
        
            <p>
                <label for="${Elements.PROJECT_INPUT.getElemName()}">Name:</label>
                <input name="${Elements.PROJECT_INPUT.getElemName()}" id="${Elements.PROJECT_INPUT.getId()}" type="text" />
            </p>
        
            <p>
                <button id="${Elements.CREATE_BUTTON.getId()}">Create new project</button>
            </p>
        
        </form>
    </body>
</html>
"""
    }
}

