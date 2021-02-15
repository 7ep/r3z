package coverosR3z.server.types

import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.timerecording.utility.ITimeRecordingUtilities

/**
 * A wrapper for the business-related objects
 */
data class BusinessCode(val tru: ITimeRecordingUtilities, val au: IAuthenticationUtilities)