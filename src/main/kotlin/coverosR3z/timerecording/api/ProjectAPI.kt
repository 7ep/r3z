package coverosR3z.timerecording.api

import coverosR3z.authentication.types.Role
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.server.utility.PageComponents
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.timerecording.types.ProjectName
import coverosR3z.timerecording.types.maxEmployeeNameSize
import coverosR3z.timerecording.types.maxProjectNameSize

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
            return doGETRequireAuth(sd.ahd.user, Role.ADMIN) { p.createProjectHTML() }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val p = ProjectAPI(sd)
            return doPOSTAuthenticated(sd.ahd.user, requiredInputs, sd.ahd.data, Role.ADMIN) { p.handlePOST() }
        }


    }

    fun handlePOST() : PreparedResponseData {
        sd.bc.tru.createProject(ProjectName.make(sd.ahd.data.mapping[Elements.PROJECT_INPUT.getElemName()]))
        return redirectTo(path)
    }


    private fun existingProjectsHtml(): String {
        val projectRows = sd.bc.tru.listAllProjects().sortedBy { it.id.value }.joinToString("") {
"""
    <tr>
        <td>${it.id.value}</td>
        <td>${safeHtml(it.name.value)}</td>
    </tr>
"""
            }

        return """
                <div class="container">
                <table>
                    <thead>
                        <tr>
                            <th>Identifier</th>
                            <th>Name</th>
                        </tr>
                    </thead>
                    <tbody>
                        $projectRows
                    </tbody>
                </table>
                </div>
        """
    }

    private fun createProjectHTML() : String {
        val body = """
            <form action="$path" method="post">
                <p>
                    <label for="${Elements.PROJECT_INPUT.getElemName()}">Name:</label>
                    <input name="${Elements.PROJECT_INPUT.getElemName()}" id="${Elements.PROJECT_INPUT.getId()}" type="text" minlength="1" maxlength="$maxProjectNameSize" required="required" />
                </p>
            
                <p>
                    <button id="${Elements.CREATE_BUTTON.getId()}">Create new project</button>
                </p>
            
            </form>
            ${existingProjectsHtml()}
"""
        return PageComponents(sd).makeTemplate("create project", "ProjectAPI", body, extraHeaderContent="""<link rel="stylesheet" href="createprojects.css" />""")
    }
}

