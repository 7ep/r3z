package coverosR3z.domainobjects

import kotlinx.serialization.Serializable

private const val maximumProjectsCount = 100_000_000
private const val maxProjectErrorMsg = "100 million projects seems too unlikely"
private const val emptyProjectNameMsg = "Makes no sense to have an empty project name"

/**
 * This is used to represent no project - just to avoid using null for a project
 * It's a typed null, essentially
 */
val NO_PROJECT = Project(maximumProjectsCount-1, "THIS REPRESENTS NO PROJECT")

/**
 * When we just have a name (like when adding a new project, or searching)
 */
@Serializable
data class ProjectName(val value: String) {
    init {
        require(value.isNotEmpty()) {emptyProjectNameMsg}
    }
}

/**
 * A full Project object
 */
@Serializable
data class Project(val id: Int, val name: String) {

    init {
        require(name.isNotEmpty()) {emptyProjectNameMsg}
        require(id < maximumProjectsCount) { maxProjectErrorMsg }
    }

}

