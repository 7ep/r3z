package coverosR3z.domainobjects

/**
 * When we just have a name (like when adding a new project, or searching)
 */
data class ProjectName(val value: String) {
    init {
        assert(value.isNotEmpty()) {"Makes no sense to have an empty project name"}
    }
}

/**
 * A full Project object
 */
data class Project(val id: Int, val name: String) {
    init {
        assert(name.isNotEmpty()) {"Makes no sense to have an empty project name"}
        assert(id < 100_000_000) { "100 million projects seems too unlikely" }
    }
}

data class ProjectId(val id: Int) {
    init {
        assert(id < 100_000_000) { "100 million projects seems too unlikely" }
    }
}