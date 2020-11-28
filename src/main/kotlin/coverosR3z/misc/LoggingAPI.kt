package coverosR3z.misc

import coverosR3z.domainobjects.User
import coverosR3z.logging.LogTypes
import coverosR3z.logging.logSettings
import coverosR3z.server.*

const val missingLoggingDataInputMsg = "input must not be missing"
const val badInputLoggingDataMsg = "input for log setting must be \"true\" or \"false\""

fun handleGETLogging(user: User): PreparedResponseData {
    return if (isAuthenticated(user)) {
        PreparedResponseData("", ResponseStatus.OK)
    } else {
        redirectTo(NamedPaths.HOMEPAGE.path)
    }
}

fun setLogging(lt : LogTypes, data: Map<String, String>) {
    when {
        checkNotNull(data[lt.toString().toLowerCase()], { missingLoggingDataInputMsg }) == "true" -> logSettings[lt] = true
        checkNotNull(data[lt.toString().toLowerCase()], { missingLoggingDataInputMsg }) == "false" -> logSettings[lt] = false
        else -> throw IllegalArgumentException(badInputLoggingDataMsg)
    }
}

fun handlePOSTLogging(user: User, data: Map<String, String>): PreparedResponseData {
    return if (isAuthenticated(user)) {
        for (lt in LogTypes.values()) {
            setLogging(lt, data)
        }
        okHTML(successHTML)
    } else {
        redirectTo(NamedPaths.HOMEPAGE.path)
    }
}