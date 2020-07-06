package com.coveros.r3z.domainobjects

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
data class Project(val id: Long, val name: String)