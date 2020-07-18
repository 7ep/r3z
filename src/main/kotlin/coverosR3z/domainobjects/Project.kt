package coverosR3z.domainobjects

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
            val matches = deserializationRegex.matchEntire(value)
            if (matches != null) {
                val (idString, name) = matches.destructured
                val id = Integer.parseInt(idString)
                return Project(id, name)
            } else {
                return null
            }
        }
    }

}

data class ProjectId(val id: Int) {
    init {
        assert(id < maximumProjectsCount) { maxProjectErrorMsg }
    }
}