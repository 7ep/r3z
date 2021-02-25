package coverosR3z

import coverosR3z.FullSystem.Companion.createSystemRunningMarker
import coverosR3z.FullSystem.Companion.startSystem
import coverosR3z.config.utility.SystemOptions.Companion.extractCommandLineOptions

/**
 * Entry point for the application.  KISS.
 */
fun main(args: Array<String>) {
    createSystemRunningMarker()
    startSystem(extractCommandLineOptions(args)).addShutdownHook()
}


