package coverosR3z

import coverosR3z.Ignition.Companion.createSystemRunningMarker
import coverosR3z.Ignition.Companion.startSystem
import coverosR3z.config.utility.SystemOptions.Companion.extractCommandLineOptions

/**
 * Entry point for the application.  KISS.
 */
fun main(args: Array<String>) {
    createSystemRunningMarker()

    val serverOptions = extractCommandLineOptions(args)

    startSystem(serverOptions)
}


