package coverosR3z

import coverosR3z.misc.testLogger
import coverosR3z.server.types.ServerObjects
import coverosR3z.techempower.utility.FakeTechempowerUtilities
import coverosR3z.uitests.Drivers

val webDriver = Drivers.CHROME

val fakeTechempower = FakeTechempowerUtilities()
val fakeServerObjects = ServerObjects(emptyMap(), testLogger, 0, 0, false)