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
        PreparedResponseData(loggingConfigHtml(), ResponseStatus.OK, listOf(ContentType.TEXT_HTML.value))
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
    logInfo("Configured logging for ${lt.name}: ${logSettings[lt]}")
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

class LogTypeState(private val logRunning : Boolean) {

    fun isOn(isRunning : Boolean = logRunning) : String {
        return if (isRunning) {
            "checked"
        } else {
            ""
        }
    }

    fun isOff() : String {
        return isOn(!logRunning)
    }

}

/**
 *
 */
fun checkedIf(lt : LogTypes) : LogTypeState {
    return LogTypeState(logSettings[lt] == true)
}



fun loggingConfigHtml() : String {
    return """
<!DOCTYPE html>
<html lang="en">
    <head>
        <title>Authenticated Homepage</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    </head>        
    <body>

        <form method="post" action="logging">
            <fieldset>
                <legend>Info logging:</legend>
                <div>
                  <input type="radio" id="infotrue" name="info" value="true" ${checkedIf(LogTypes.INFO).isOn()} >
                  <label for="infotrue">True</label>
            
                  <input type="radio" id="infofalse" name="info" value="false" ${checkedIf(LogTypes.INFO).isOff()}>
                  <label for="infofalse">False</label>
                </div>
            </fieldset>

            <fieldset>
                <legend>Warn logging:</legend>
                <div>
                  <input type="radio" id="warntrue" name="warn" value="true" ${checkedIf(LogTypes.WARN).isOn()}>
                  <label for="warntrue">True</label>
            
                  <input type="radio" id="warnfalse" name="warn" value="false" ${checkedIf(LogTypes.WARN).isOff()}>
                  <label for="warnfalse">False</label>
                </div>
            </fieldset>

            <fieldset>
                <legend>Debug logging:</legend>
                <div>
                  <input type="radio" id="debugtrue" name="debug" value="true" ${checkedIf(LogTypes.DEBUG).isOn()}>
                  <label for="debugtrue">True</label>
            
                  <input type="radio" id="debugfalse" name="debug" value="false" ${checkedIf(LogTypes.DEBUG).isOff()}>
                  <label for="debugfalse">False</label>
                </div>
            </fieldset>
            
            <fieldset>
                <legend>Trace logging:</legend>
                <div>
                  <input type="radio" id="tracetrue" name="trace" value="true" ${checkedIf(LogTypes.TRACE).isOn()}>
                  <label for="tracetrue">True</label>
            
                  <input type="radio" id="tracefalse" name="trace" value="false" ${checkedIf(LogTypes.TRACE).isOff()}>
                  <label for="tracefalse">False</label>
                </div>
            </fieldset>
            
            <button>Save</button>
        </form>


    </body>
</html>

"""
}