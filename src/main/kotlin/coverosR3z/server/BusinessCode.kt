package coverosR3z.server

import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.timerecording.ITimeRecordingUtilities

/**
 * A wrapper for the business-related instantiations
 */
data class BusinessCode(val tru: ITimeRecordingUtilities, val au: IAuthenticationUtilities)