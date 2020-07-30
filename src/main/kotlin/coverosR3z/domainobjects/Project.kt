package coverosR3z.domainobjects

import coverosR3z.exceptions.MalformedDataDuringSerializationException
import kotlinx.serialization.Serializable
import java.lang.Integer.parseInt

private const val maximumProjectsCount = 100_000_000
private const val maxProjectErrorMsg = "100 million projects seems too unlikely"
private const val emptyProjectNameMsg = "Makes no sense to have an empty project name"

/**
 * When we just have a name (like when adding a new project, or searching)
 */
@Serializable
data class ProjectName(val value: String) {
    init {
        assert(value.isNotEmpty()) {emptyProjectNameMsg}
    }
}

/**
 * A full Project object
 */
@Serializable
data class Project(val id: Int, val name: String) {

    init {
        assert(name.isNotEmpty()) {emptyProjectNameMsg}
        assert(id < maximumProjectsCount) { maxProjectErrorMsg }
    }

}

@Serializable
data class ProjectId(val id: Int) {
    init {
        assert(id < maximumProjectsCount) { maxProjectErrorMsg }
    }
}