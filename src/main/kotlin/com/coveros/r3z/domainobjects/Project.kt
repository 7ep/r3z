package com.coveros.r3z.domainobjects

data class ProjectName(val value: String) {
    init {
        assert(value.isNotEmpty()) {"Makes no sense to have an empty project name"}
    }
}
data class Project(val id: Long, val name: String)