package coverosR3z

import coverosR3z.misc.testLogger
import coverosR3z.server.types.ServerObjects
import coverosR3z.system.FakeSystemConfigurationPersistence
import coverosR3z.uitests.Drivers

val webDriver = Drivers.CHROME

val fakeServerObjects = ServerObjects(emptyMap(), testLogger, 0, 0, false, scp = FakeSystemConfigurationPersistence())