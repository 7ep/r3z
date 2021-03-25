package coverosR3z.logging

import coverosR3z.authentication.types.Role
import coverosR3z.logging.ILogger.Companion.logImperative
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import coverosR3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import coverosR3z.server.utility.PageComponents
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo


class LoggingAPI(private val sd: ServerData) {

    enum class Elements(private val elemName: String, private val id: String) : Element {
        AUDIT_INPUT("audit", "audit"),
        DEBUG_INPUT("debug", "debug"),
        WARN_INPUT("warn", "warn"),
        TRACE_INPUT("trace", "trace"),
        SAVE_BUTTON("", "save"),;

        override fun getId(): String {
            return this.id
        }

        override fun getElemName(): String {
            return this.elemName
        }

        override fun getElemClass(): String {
            throw NotImplementedError()
        }
    }

    companion object : GetEndpoint, PostEndpoint {
        private const val missingLoggingDataInputMsg = "input must not be missing"
        const val badInputLoggingDataMsg = "input for log setting must be \"true\" or \"false\""

        override val requiredInputs = setOf(
            Elements.AUDIT_INPUT,
            Elements.WARN_INPUT,
            Elements.DEBUG_INPUT,
            Elements.TRACE_INPUT,
        )
        override val path: String
            get() = "logging"

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val l = LoggingAPI(sd)
            return doGETRequireAuth(sd.ahd.user, Role.ADMIN) { l.loggingConfigHtml() }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            val l = LoggingAPI(sd)
            return doPOSTAuthenticated(sd.ahd.user, requiredInputs, sd.ahd.data, Role.ADMIN) { l.handlePOST() }
        }

    }

    private fun setLogging(lt : LogTypes, data: PostBodyData) {
        /**
         * The user has sent us a map with four keys - audit, warn, debug, trace,
         * each of which can be "true" or "false".  This is that value.
         */
        val userInputLogConfig = checkNotNull(data.mapping[lt.toString().toLowerCase()], { missingLoggingDataInputMsg })

        // depending on what the user chose, we set the configuration for that logging type here
        when (userInputLogConfig) {
            "true" -> sd.logger.logSettings[lt] = true
            "false" -> sd.logger.logSettings[lt] = false
            else -> throw IllegalArgumentException(badInputLoggingDataMsg)
        }
        logImperative("Configured logging for ${lt.name}: ${sd.logger.logSettings[lt]}")
    }

    fun handlePOST() : PreparedResponseData {
        for (lt in LogTypes.values()) {
            setLogging(lt, sd.ahd.data)
        }
        return redirectTo(path)
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
        return LogTypeState(sd.logger.logSettings[lt] == true)
    }



    private fun loggingConfigHtml() : String {
        val body = """
            <div class="container">
                <form method="post" action="$path">
                    <fieldset>
                    
                        <legend>Audit logging:</legend>
                        
                          <input type="radio" id="${Elements.AUDIT_INPUT.getId()}true" name="${Elements.AUDIT_INPUT.getElemName()}" value="true" ${checkedIf(LogTypes.AUDIT).isOn()} >
                          <label for="${Elements.AUDIT_INPUT.getId()}true">True</label>
                    
                          <input type="radio" id="${Elements.AUDIT_INPUT.getId()}false" name="${Elements.AUDIT_INPUT.getElemName()}" value="false" ${checkedIf(LogTypes.AUDIT).isOff()}>
                          <label for="${Elements.AUDIT_INPUT.getId()}false">False</label>
                          
                    </fieldset>
        
                    <fieldset>
                    
                        <legend>Warn logging:</legend>
                        
                          <input type="radio" id="${Elements.WARN_INPUT.getId()}true" name="${Elements.WARN_INPUT.getElemName()}" value="true" ${checkedIf(LogTypes.WARN).isOn()}>
                          <label for="${Elements.WARN_INPUT.getId()}true">True</label>
                    
                          <input type="radio" id="${Elements.WARN_INPUT.getId()}false" name="${Elements.WARN_INPUT.getElemName()}" value="false" ${checkedIf(LogTypes.WARN).isOff()}>
                          <label for="${Elements.WARN_INPUT.getId()}false">False</label>
                          
                    </fieldset>
        
                    <fieldset>
                    
                        <legend>Debug logging:</legend>
                        
                          <input type="radio" id="${Elements.DEBUG_INPUT.getId()}true" name="${Elements.DEBUG_INPUT.getElemName()}" value="true" ${checkedIf(LogTypes.DEBUG).isOn()}>
                          <label for="${Elements.DEBUG_INPUT.getId()}true">True</label>
                    
                          <input type="radio" id="${Elements.DEBUG_INPUT.getId()}false" name="${Elements.DEBUG_INPUT.getElemName()}" value="false" ${checkedIf(LogTypes.DEBUG).isOff()}>
                          <label for="${Elements.DEBUG_INPUT.getId()}false">False</label>
                          
                    </fieldset>
                    
                    <fieldset>
                    
                        <legend>Trace logging:</legend>
                        
                        <input type="radio" id="${Elements.TRACE_INPUT.getId()}true" name="${Elements.TRACE_INPUT.getElemName()}" value="true" ${checkedIf(LogTypes.TRACE).isOn()}>
                        <label for="${Elements.TRACE_INPUT.getId()}true">True</label>
                        
                        <input type="radio" id="${Elements.TRACE_INPUT.getId()}false" name="${Elements.TRACE_INPUT.getElemName()}" value="false" ${checkedIf(LogTypes.TRACE).isOff()}>
                        <label for="${Elements.TRACE_INPUT.getId()}false">False</label>
                        
                    </fieldset>
                    
                    <button id="${Elements.SAVE_BUTTON.getId()}">Save</button>
                </form>
            </div>
    """
        return PageComponents(sd).makeTemplate("Logging configuration", "LoggingAPI", body, extraHeaderContent="""<link rel="stylesheet" href="loggingconfig.css" />""")
    }
}