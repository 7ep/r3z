package coverosR3z.domainobjects

import coverosR3z.exceptions.MalformedDataDuringSerializationException
import java.lang.Integer.parseInt

private const val maximumProjectsCount = 100_000_000
private const val maxProjectErrorMsg = "100 million projects seems too unlikely"
private const val emptyProjectNameMsg = "Makes no sense to have an empty project name"

/**
 * When we just have a name (like when adding a new project, or searching)
 */
data class ProjectName(val value: String) {
    init {
        assert(value.isNotEmpty()) {emptyProjectNameMsg}
    }
}

/**
 * A full Project object
 */
data class Project(val id: Int, val name: String) {

    init {
        assert(name.isNotEmpty()) {emptyProjectNameMsg}
        assert(id < maximumProjectsCount) { maxProjectErrorMsg }
    }

    fun serialize(): String {
        return "{id=$id,name=$name}"
    }

    companion object {
        private val deserializationRegex = "\\{id=(.*),name=(.*)}".toRegex()

        fun deserialize(value : String) : Project? {
            try {
                val matches = deserializationRegex.matchEntire(value) ?: throw Exception()
                val (idString, name) = matches.destructured
                val id = parseInt(idString)
                return Project(id, name)
            } catch (ex : Exception) {
                throw MalformedDataDuringSerializationException("was unable to deserialize this: ( $value )")
            }
        }
    }

}

data class ProjectId(val id: Int) {
    init {
        assert(id < maximumProjectsCount) { maxProjectErrorMsg }
    }
}