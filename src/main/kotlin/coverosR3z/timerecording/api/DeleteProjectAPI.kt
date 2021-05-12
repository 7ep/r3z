package coverosR3z.timerecording.api

import coverosR3z.authentication.types.Role
import coverosR3z.server.api.MessageAPI
import coverosR3z.server.api.MessageAPI.Companion.createEnumMessageRedirect
import coverosR3z.server.types.Element
import coverosR3z.server.types.PostEndpoint
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.timerecording.types.DeleteProjectResult
import coverosR3z.timerecording.types.NO_PROJECT
import coverosR3z.timerecording.types.ProjectId

class DeleteProjectAPI {

    companion object : PostEndpoint {

        override fun handlePost(sd: ServerData): PreparedResponseData {
            return doPOSTAuthenticated(sd, requiredInputs, ProjectAPI.path, Role.ADMIN) {
                val projectId = ProjectId.make(sd.ahd.data.mapping[Elements.ID.getElemName()])
                val project = sd.bc.tru.findProjectById(projectId)
                if (project == NO_PROJECT) {
                    throw IllegalStateException("No project found by that id")
                }
                when(sd.bc.tru.deleteProject(project)) {
                    DeleteProjectResult.SUCCESS -> createEnumMessageRedirect(MessageAPI.Message.PROJECT_DELETED)
                    DeleteProjectResult.USED -> createEnumMessageRedirect(MessageAPI.Message.PROJECT_USED)
                }
            }
        }

        override val requiredInputs: Set<Element> = setOf(Elements.ID)
        override val path: String = "deleteproject"

    }

    enum class Elements(private val value: String = "") : Element {
        ID("id");
        override fun getId(): String {
            return this.value
        }

        override fun getElemName(): String {
            return this.value
        }

        override fun getElemClass(): String {
            return this.value
        }
    }

}
