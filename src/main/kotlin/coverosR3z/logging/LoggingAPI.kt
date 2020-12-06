package coverosR3z.logging

import coverosR3z.domainobjects.User
import coverosR3z.misc.successHTML
import coverosR3z.server.*

const val missingLoggingDataInputMsg = "input must not be missing"
const val badInputLoggingDataMsg = "input for log setting must be \"true\" or \"false\""

fun handleGETLogging(user: User): PreparedResponseData {
    return if (isAuthenticated(user)) {
        PreparedResponseData(loggingConfigHtml(), StatusCode.OK, listOf(ContentType.TEXT_HTML.value))
    } else {
        redirectTo(NamedPaths.HOMEPAGE.path)
    }
}

fun setLogging(lt : LogTypes, data: Map<String, String>) {
    /**
     * The user has sent us a map with four keys - audit, warn, debug, trace,
     * each of which can be "true" or "false".  This is that value.
     */
    val userInputLogConfig = checkNotNull(data[lt.toString()], { missingLoggingDataInputMsg })

    // depending on what the user chose, we set the configuration for that logging type here
    when (userInputLogConfig) {
        "true" -> logSettings[lt] = true
        "false" -> logSettings[lt] = false
        else -> throw IllegalArgumentException(badInputLoggingDataMsg)
    }
    logDebug("Configured logging for ${lt.name}: ${logSettings[lt]}")
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
                  <input type="radio" id="audittrue" name="${LogTypes.AUDIT}" value="true" ${checkedIf(LogTypes.AUDIT).isOn()} >
                  <label for="audittrue">True</label>
            
                  <input type="radio" id="auditfalse" name="${LogTypes.AUDIT}" value="false" ${checkedIf(LogTypes.AUDIT).isOff()}>
                  <label for="auditfalse">False</label>
                </div>
            </fieldset>

            <fieldset>
                <legend>Warn logging:</legend>
                <div>
                  <input type="radio" id="warntrue" name="${LogTypes.WARN}" value="true" ${checkedIf(LogTypes.WARN).isOn()}>
                  <label for="warntrue">True</label>
            
                  <input type="radio" id="warnfalse" name="${LogTypes.WARN}" value="false" ${checkedIf(LogTypes.WARN).isOff()}>
                  <label for="warnfalse">False</label>
                </div>
            </fieldset>

            <fieldset>
                <legend>Debug logging:</legend>
                <div>
                  <input type="radio" id="debugtrue" name="${LogTypes.DEBUG}" value="true" ${checkedIf(LogTypes.DEBUG).isOn()}>
                  <label for="debugtrue">True</label>
            
                  <input type="radio" id="debugfalse" name="${LogTypes.DEBUG}" value="false" ${checkedIf(LogTypes.DEBUG).isOff()}>
                  <label for="debugfalse">False</label>
                </div>
            </fieldset>
            
            <fieldset>
                <legend>Trace logging:</legend>
                <div>
                  <input type="radio" id="tracetrue" name="${LogTypes.TRACE}" value="true" ${checkedIf(LogTypes.TRACE).isOn()}>
                  <label for="tracetrue">True</label>
            
                  <input type="radio" id="tracefalse" name="${LogTypes.TRACE}" value="false" ${checkedIf(LogTypes.TRACE).isOff()}>
                  <label for="tracefalse">False</label>
                </div>
            </fieldset>
            
            <button>Save</button>
        </form>


    </body>
</html>

"""
}