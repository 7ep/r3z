package coverosR3z.system.persistence

import coverosR3z.system.types.SystemConfiguration

interface ISystemConfigurationPersistence {
    fun setSystemConfig(sysConfig: SystemConfiguration)
    fun getSystemConfig(): SystemConfiguration?

}
