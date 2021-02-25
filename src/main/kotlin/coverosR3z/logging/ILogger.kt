package coverosR3z.logging

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.config.utility.SystemOptions
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

interface ILogger {
    val logSettings: MutableMap<LogTypes, Boolean>

    /**
     * Set the system to standard configuration for which
     * log entries will print
     */
    fun resetLogSettingsToDefault()
    fun turnOnAllLogging()
    fun turnOffAllLogging()
    fun logAudit(cu: CurrentUser = CurrentUser(SYSTEM_USER), msg: () -> String)

    /**
     * Used to log finicky details of technical solutions
     */
    fun logDebug(cu: CurrentUser = CurrentUser(SYSTEM_USER), msg: () -> String)

    /**
     * Logs nearly extraneous levels of detail.
     */
    fun logTrace(cu: CurrentUser = CurrentUser(SYSTEM_USER), msg: () -> String)

    /**
     * Logs items that could be concerning to the operations team.  Like
     * a missing database file.
     */
    fun logWarn(cu: CurrentUser = CurrentUser(SYSTEM_USER), msg: () -> String)

    /**
     * Sets logging per the [SystemOptions], if the user requested
     * something
     */
    fun configureLogging(serverOptions: SystemOptions)
    fun stop()

    companion object {
        /**
         * Logging that must be shown, which you cannot turn off
         */
        fun logImperative(msg: String) {
            println("${getTimestamp()} IMPERATIVE: $msg")
        }

        fun getCurrentMillis() : Long {
            return System.currentTimeMillis()
        }

        fun getTimestamp() : String {
            return ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)
        }
    }
}