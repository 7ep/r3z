package coverosR3z.logging

import coverosR3z.misc.successHTML
import coverosR3z.server.*



class LoggingAPI {

    enum class Elements(val elemName: String, val id: String) {
        AUDIT_INPUT("audit", "audit"),
        DEBUG_INPUT("debug", "debug"),
        WARN_INPUT("warn", "warn"),
        TRACE_INPUT("trace", "trace"),
    }

    companion object {
        const val missingLoggingDataInputMsg = "input must not be missing"
        const val badInputLoggingDataMsg = "input for log setting must be \"true\" or \"false\""

        val requiredInputs = setOf(
            Elements.AUDIT_INPUT.elemName,
            Elements.WARN_INPUT.elemName,
            Elements.DEBUG_INPUT.elemName,
            Elements.TRACE_INPUT.elemName,
        )

        fun generateLoggingConfigPage(): String = loggingConfigHtml()

        fun setLogging(lt : LogTypes, data: Map<String, String>) {
            /**
             * The user has sent us a map with four keys - audit, warn, debug, trace,
             * each of which can be "true" or "false".  This is that value.
             */
            val userInputLogConfig = checkNotNull(data[lt.toString().toLowerCase()], { missingLoggingDataInputMsg })

            // depending on what the user chose, we set the configuration for that logging type here
            when (userInputLogConfig) {
                "true" -> logSettings[lt] = true
                "false" -> logSettings[lt] = false
                else -> throw IllegalArgumentException(badInputLoggingDataMsg)
            }
            logDebug("Configured logging for ${lt.name}: ${logSettings[lt]}")
        }

        fun handlePOST(data: Map<String, String>) : PreparedResponseData{
            for (lt in LogTypes.values()) {
                setLogging(lt, data)
            }
            return okHTML(successHTML)
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
        private fun checkedIf(lt : LogTypes) : LogTypeState {
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
                          <input type="radio" id="audittrue" name="${Elements.AUDIT_INPUT.elemName}" value="true" ${checkedIf(LogTypes.AUDIT).isOn()} >
                          <label for="audittrue">True</label>
                    
                          <input type="radio" id="auditfalse" name="${Elements.AUDIT_INPUT.elemName}" value="false" ${checkedIf(LogTypes.AUDIT).isOff()}>
                          <label for="auditfalse">False</label>
                        </div>
                    </fieldset>
        
                    <fieldset>
                        <legend>Warn logging:</legend>
                        <div>
                          <input type="radio" id="warntrue" name="${Elements.WARN_INPUT.elemName}" value="true" ${checkedIf(LogTypes.WARN).isOn()}>
                          <label for="warntrue">True</label>
                    
                          <input type="radio" id="warnfalse" name="${Elements.WARN_INPUT.elemName}" value="false" ${checkedIf(LogTypes.WARN).isOff()}>
                          <label for="warnfalse">False</label>
                        </div>
                    </fieldset>
        
                    <fieldset>
                        <legend>Debug logging:</legend>
                        <div>
                          <input type="radio" id="debugtrue" name="${Elements.DEBUG_INPUT.elemName}" value="true" ${checkedIf(LogTypes.DEBUG).isOn()}>
                          <label for="debugtrue">True</label>
                    
                          <input type="radio" id="debugfalse" name="${Elements.DEBUG_INPUT.elemName}" value="false" ${checkedIf(LogTypes.DEBUG).isOff()}>
                          <label for="debugfalse">False</label>
                        </div>
                    </fieldset>
                    
                    <fieldset>
                        <legend>Trace logging:</legend>
                        <div>
                          <input type="radio" id="tracetrue" name="${Elements.TRACE_INPUT.elemName}" value="true" ${checkedIf(LogTypes.TRACE).isOn()}>
                          <label for="tracetrue">True</label>
                    
                          <input type="radio" id="tracefalse" name="${Elements.TRACE_INPUT.elemName}" value="false" ${checkedIf(LogTypes.TRACE).isOff()}>
                          <label for="tracefalse">False</label>
                        </div>
                    </fieldset>
                    
                    <button>Save</button>
                </form>
        
        
            </body>
        </html>
        
        """
        }
    }
}