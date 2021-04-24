package coverosR3z.system.config.persistence

import coverosR3z.persistence.types.DataAccess
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.system.config.types.SystemConfiguration

/**
 * The system configuration stores those pieces of data that can be
 * altered during the run-time of this application, most likely by
 * an administrator of the system.  An example would be the logging.
 *
 * There can only be one
 */
class SystemConfigurationPersistence(pmd : PureMemoryDatabase) : ISystemConfigurationPersistence {

    private val configurationDataAccess: DataAccess<SystemConfiguration> = pmd.dataAccess(SystemConfiguration.directoryName)

    override fun setSystemConfig(sysConfig: SystemConfiguration) {
        configurationDataAccess.actOn {
            // if it's never been set before, add this
            if (it.singleOrNull() == null) {
                it.nextIndex.getAndIncrement()
                it.add(sysConfig)
            } else {
                // otherwise, update the existing config
                it.update(sysConfig)
            }
        }
    }

    override fun getSystemConfig(): SystemConfiguration? {
        return configurationDataAccess.read { it.singleOrNull() }
    }
}