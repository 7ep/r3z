package coverosR3z.timerecording.types

import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.persistence.types.Deserializable
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializableCompanion
import coverosR3z.persistence.types.SerializationKeys
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.dbentryDeserialize

const val maximumProjectsCount = 100_000_000
private const val maxProjectNameSize = 30
const val maxProjectNameSizeMsg = "Max size of project name is $maxProjectNameSize"
private const val maxProjectErrorMsg = "No project id allowed over $maximumProjectsCount"
private const val emptyProjectNameMsg = "Makes no sense to have an empty project name"
private const val minIdMsg = "Valid identifier values are 1 or above"
const val projectNameNotNullMsg = "The project name must not be null"

/**
 * This is used to represent no project - just to avoid using null for a project
 * It's a typed null, essentially
 */
val NO_PROJECT = Project(ProjectId(maximumProjectsCount -1), ProjectName("THIS REPRESENTS NO PROJECT"))

/**
 * When we just have a name (like when adding a new project, or searching)
 */
data class ProjectName(val value: String) {
    init {
        require(value.isNotEmpty()) { emptyProjectNameMsg }
        require(value.length <= maxProjectNameSize) { maxProjectNameSizeMsg }
    }

    companion object {
        fun make(value: String?) : ProjectName {
            val valueNotNull = checkNotNull(value) { projectNameNotNullMsg }
            return ProjectName(valueNotNull)
        }
    }
}

data class ProjectId(val value: Int) {
    init {
        require(value > 0) { minIdMsg }
        require(value < maximumProjectsCount) { maxProjectErrorMsg }
    }

    companion object {
        fun make(value: String?) : ProjectId {
            return ProjectId(checkParseToInt(value))
        }
    }
}

/**
 * A full Project object
 */
data class Project(val id: ProjectId, val name: ProjectName) : IndexableSerializable() {

    override fun getIndex(): Int {
        return id.value
    }

    override val dataMappings: Map<SerializationKeys, String>
        get() = mapOf(
            Keys.ID to "${id.value}",
            Keys.NAME to name.value
        )

    class Deserializer : Deserializable<Project> {

        override fun deserialize(str: String) : Project {
            return dbentryDeserialize(str, Companion) { entries ->
                val id = checkParseToInt(entries[Keys.ID])
                Project(ProjectId(id), ProjectName.make(entries[Keys.NAME]))
            }
        }
    }

    companion object : SerializableCompanion<Keys>(Keys.values()) {

        override val directoryName: String
            get() = "projects"
    }

    enum class Keys(override val keyString: String) : SerializationKeys {
        ID("id"),
        NAME("name");
    }

}

