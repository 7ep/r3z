package coverosR3z.server.types

import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.server.types.AnalyzedHttpData
import coverosR3z.timerecording.utility.ITimeRecordingUtilities

/**
 * Data for use by the API endpoints
 */
data class ServerData(val au: IAuthenticationUtilities, val tru: ITimeRecordingUtilities, val ahd: AnalyzedHttpData, val authStatus: AuthStatus)