package coverosR3z.system

import coverosR3z.system.config.persistence.ISystemConfigurationPersistence
import coverosR3z.system.config.types.SystemConfiguration

class FakeSystemConfigurationPersistence(
    var setSystemConfigBehavior: () -> Unit = {},
    var getSystemConfigBehavior: () -> SystemConfiguration = { SystemConfiguration(SystemConfiguration.LogSettings(audit = true, warn = true, debug = true, trace = true)) }
) : ISystemConfigurationPersistence {

    override fun setSystemConfig(sysConfig: SystemConfiguration) {
        setSystemConfigBehavior()
    }

    override fun getSystemConfig(): SystemConfiguration {
        return getSystemConfigBehavior()
    }
}