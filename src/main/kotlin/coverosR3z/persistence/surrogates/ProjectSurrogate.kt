package coverosR3z.persistence.surrogates

import coverosR3z.domainobjects.Project
import coverosR3z.domainobjects.ProjectId
import coverosR3z.domainobjects.ProjectName
import coverosR3z.exceptions.DatabaseCorruptedException
import coverosR3z.misc.checkParseToInt
import coverosR3z.misc.decode
import coverosR3z.misc.encode

/**
 * A surrogate. See longer description for another surrogate at [TimeEntrySurrogate]
 */
data class ProjectSurrogate(val id: Int, val name: String) {

    fun serialize(): String {
        return """{ id: $id , name: ${encode(name)} }"""
    }

    companion object {

        fun deserialize(str: String): ProjectSurrogate {
            try {
                val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap{it.groupValues}.toList()
                val id = checkParseToInt(groups[1])
                return ProjectSurrogate(id, decode(groups[3]))
            } catch (ex : Throwable) {
                throw DatabaseCorruptedException("Unable to deserialize this text as project data: $str", ex)
            }
        }

        fun deserializeToProject(str: String) : Project {
            return fromSurrogate(deserialize(str))
        }

        fun toSurrogate(p : Project) : ProjectSurrogate {
            return ProjectSurrogate(p.id.value, p.name.value)
        }

        private fun fromSurrogate(ps: ProjectSurrogate) : Project {
            return Project(ProjectId(ps.id), ProjectName(ps.name))
        }
    }
}