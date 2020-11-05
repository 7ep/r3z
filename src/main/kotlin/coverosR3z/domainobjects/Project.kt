package coverosR3z.domainobjects

import kotlinx.serialization.Serializable

private const val maximumProjectsCount = 100_000_000
private const val maxProjectErrorMsg = "100 million projects seems too unlikely"
private const val emptyProjectNameMsg = "Makes no sense to have an empty project name"

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

@Serializable
data class ProjectId(val id: Int) {
    init {
        require(id < maximumProjectsCount) { maxProjectErrorMsg }
    }
}