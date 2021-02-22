package coverosR3z.server.types

import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.logging.ILogger
import coverosR3z.timerecording.utility.ITimeRecordingUtilities

/**
 * Data for use by the API endpoints
 */
data class ServerData(
    val bc: BusinessCode,
    val so: ServerObjects,
    val ahd: AnalyzedHttpData,
    val authStatus: AuthStatus,
    val logger: ILogger,
)