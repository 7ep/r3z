package coverosR3z.system.config.persistence

import coverosR3z.system.config.types.SystemConfiguration

interface ISystemConfigurationPersistence {
    fun setSystemConfig(sysConfig: SystemConfiguration)
    fun getSystemConfig(): SystemConfiguration?

}
