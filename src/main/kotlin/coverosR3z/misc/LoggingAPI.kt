package coverosR3z.misc

import coverosR3z.domainobjects.User
import coverosR3z.logging.LogTypes
import coverosR3z.logging.logInfo
import coverosR3z.logging.logSettings
import coverosR3z.server.*

const val missingLoggingDataInputMsg = "input must not be missing"
const val badInputLoggingDataMsg = "input for log setting must be \"true\" or \"false\""

fun handleGETLogging(user: User): PreparedResponseData {
    return if (isAuthenticated(user)) {
        PreparedResponseData(loggingConfigHtml, ResponseStatus.OK)
    } else {
        redirectTo(NamedPaths.HOMEPAGE.path)
    }
}

fun setLogging(lt : LogTypes, data: Map<String, String>) {
    /**
     * The user has sent us a map with four keys - info, warn, debug, trace,
     * each of which can be "true" or "false".  This is that value.
     */
    val userInputLogConfig = checkNotNull(data[lt.toString().toLowerCase()], { missingLoggingDataInputMsg })

    // depending on what the user chose, we set the configuration for that logging type here
    when (userInputLogConfig) {
        "true" -> logSettings[lt] = true
        "false" -> logSettings[lt] = false
        else -> throw IllegalArgumentException(badInputLoggingDataMsg)
    }
    logInfo("Configured logging for ${lt.name}: ${lt.toString().toLowerCase()}")
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

const val loggingConfigHtml = """
<!DOCTYPE html>
<html lang="en">
    <head>
        <title>Authenticated Homepage</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    </head>        
    <body>

        <form method="post" action="logging">
            <p>Info logging:</p>

            <div>
              <input type="radio" id="infotrue" name="info" value="true" checked>
              <label for="infotrue">True</label>
        
              <input type="radio" id="infofalse" name="info" value="false">
              <label for="infofalse">False</label>
            </div>

            <p>Warn logging:</p>

            <div>
              <input type="radio" id="warntrue" name="warn" value="true" checked>
              <label for="warntrue">True</label>
        
              <input type="radio" id="warnfalse" name="warn" value="false">
              <label for="warnfalse">False</label>
            </div>

            <p>Debug logging:</p>

            <div>
              <input type="radio" id="debugtrue" name="debug" value="true">
              <label for="debugtrue">True</label>
        
              <input type="radio" id="debugfalse" name="debug" value="false" checked>
              <label for="debugfalse">False</label>
            </div>

            <p>Trace logging:</p>

            <div>
              <input type="radio" id="tracetrue" name="trace" value="true">
              <label for="tracetrue">True</label>
        
              <input type="radio" id="tracefalse" name="trace" value="false" checked>
              <label for="tracefalse">False</label>
            </div>
            
            <button>Save</button>
        </form>


    </body>
</html>

"""