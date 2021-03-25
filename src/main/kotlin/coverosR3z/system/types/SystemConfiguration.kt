package coverosR3z.system.types

import coverosR3z.persistence.types.Deserializable
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializableCompanion
import coverosR3z.persistence.types.SerializationKeys
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.dbentryDeserialize

/**
 * A type to store all the system's configurations that
 * can be altered during runtime
 */
data class SystemConfiguration(
    val logSettings: LogSettings
    )  : IndexableSerializable(){

    data class LogSettings(val audit: Boolean, val warn: Boolean, val debug: Boolean, val trace: Boolean)

    enum class Keys(override val keyString: String) : SerializationKeys {
        ID("id"),
        LOGGING_AUDIT("la"),
        LOGGING_WARN("lw"),
        LOGGING_DEBUG("ld"),
        LOGGING_TRACE("lt"),
    }

    override fun getIndex(): Int {
        // there will only ever be one
        return 1
    }

    override val dataMappings: Map<SerializationKeys, String>
        get() = mapOf(
            Keys.ID to "${getIndex()}",
            Keys.LOGGING_AUDIT to "${logSettings.audit}",
            Keys.LOGGING_WARN to "${logSettings.warn}",
            Keys.LOGGING_DEBUG to "${logSettings.debug}",
            Keys.LOGGING_TRACE to "${logSettings.trace}",
        )

    class Deserializer : Deserializable<SystemConfiguration> {

        override fun deserialize(str: String): SystemConfiguration {
            return dbentryDeserialize(str, Companion) { entries ->
                val logSettings =  LogSettings(
                    entries[Keys.LOGGING_AUDIT].toBoolean(),
                    entries[Keys.LOGGING_WARN].toBoolean(),
                    entries[Keys.LOGGING_DEBUG].toBoolean(),
                    entries[Keys.LOGGING_TRACE].toBoolean())

                SystemConfiguration(
                    logSettings
                )
            }
        }
    }

    companion object : SerializableCompanion<Keys>(Keys.values()) {
        override val directoryName: String
            get() = "system_configuration"

    }


}
