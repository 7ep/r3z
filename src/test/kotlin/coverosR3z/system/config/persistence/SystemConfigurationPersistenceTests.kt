package coverosR3z.system.config.persistence

import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.system.config.types.SystemConfiguration
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemConfigurationPersistenceTests {

    @Test
    fun testSystemConfigurationPersistence() {
        val sysConfigPersistence = SystemConfigurationPersistence(PureMemoryDatabase.createEmptyDatabase())
        val sysConfig = SystemConfiguration(
            SystemConfiguration.LogSettings(audit = true, warn = true, debug = true, trace = true)
        )

        // persist a totally new configuration
        sysConfigPersistence.setSystemConfig(sysConfig)
        val newConfig = sysConfigPersistence.getSystemConfig()

        // check it's what we expect
        assertEquals(sysConfig, newConfig)

        // persist an updated configuration
        sysConfigPersistence.setSystemConfig(sysConfig)
        val newConfig2 = sysConfigPersistence.getSystemConfig()

        // check it's what we expect
        assertEquals(sysConfig, newConfig2)
    }
}