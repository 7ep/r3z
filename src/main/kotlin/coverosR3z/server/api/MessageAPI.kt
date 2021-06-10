package coverosR3z.server.api

import coverosR3z.authentication.api.LoginAPI
import coverosR3z.server.types.*
import coverosR3z.server.utility.PageComponents
import coverosR3z.server.utility.ServerUtilities
import coverosR3z.system.logging.LoggingAPI
import coverosR3z.system.misc.utility.decode
import coverosR3z.system.misc.utility.encode
import coverosR3z.system.misc.utility.safeAttr
import coverosR3z.system.misc.utility.safeHtml
import coverosR3z.timerecording.api.CreateEmployeeAPI
import coverosR3z.timerecording.api.ProjectAPI
import coverosR3z.timerecording.api.ViewTimeAPI
import java.lang.IllegalArgumentException

/**
 * This is used to show messages to the user after
 * they POST data to the server.  This way, we are
 * able to provide a clear and simple page just for indicating
 * critical messages, and then provide a link
 * back to where they came from, to avoid that nonsense
 * of the user reloading the page and causing the action
 * to take place again.
 */
class MessageAPI {


    companion object : GetEndpoint {

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val message = extractMessage(sd)

            /**
             * Code for rendering a message
             */
            val body = """
                <div class="container">
                    <div id="message_text" class="${message.type.toString().toLowerCase()}" >${safeHtml(message.text)}</div>
                    <a class="button" href="${safeAttr(message.returnLink)}">OK</a>
                </div>
                """.trimIndent()

            val fullTemplate = PageComponents(sd).makeTemplate(title = message.text,"MessageAPI",body = body,"""<link rel="stylesheet" href="message.css" />""")
            return PreparedResponseData(fullTemplate, message.statusCode, listOf(ContentType.TEXT_HTML.value))
        }

        /**
         * Examine the query string and determine what to show the user
         */
        private fun extractMessage(sd: ServerData) : IMessage {
            val enumMessage = try {
                // either get the value, or if null, return NO_MESSAGE
                Message.valueOf(sd.ahd.queryString[enumeratedMessageKey] ?: Message.NO_MESSAGE.toString())
            } catch (ex: IllegalArgumentException) {
                // or if the value doesn't match anything, return NO_MATCHING_MESSAGE
                Message.NO_MATCHING_MESSAGE
            }

            val hasEnumMessage = enumMessage != Message.NO_MATCHING_MESSAGE && enumMessage != Message.NO_MESSAGE

            val customMessageCode = sd.ahd.queryString[customMessageKey]
            val customReturnLink = sd.ahd.queryString[customReturnLinkKey] ?: "homepage"
            val isSuccess = sd.ahd.queryString[customIsSuccessKey].toBoolean()

            val hasCustomMessage = (! customMessageCode.isNullOrBlank())

            return if (hasEnumMessage && hasCustomMessage) {
                Message.MIXED_MESSAGE_TYPES
            } else if (hasEnumMessage) {
                // at this point, we have either an enumerated message or a custom message, but not both
                enumMessage
            } else if (hasCustomMessage) {
                val messageType = if (isSuccess) MessageType.SUCCESS else MessageType.FAILURE
                CustomMessage(decode(customMessageCode), decode(customReturnLink), messageType, StatusCode.OK)
            } else {
                enumMessage
            }
        }

        override val path: String = "result"

        /**
         * If the message is one of an enumerated list of potential messages.  See [Message]
         */
        const val enumeratedMessageKey: String = "msg"

        /**
         * If we are sending URL-encoded text to be shown.
         */
        const val customMessageKey: String = "custommsg"
        const val customReturnLinkKey: String = "rtn"
        const val customIsSuccessKey: String = "suc"

        /**
         * Redirects to the MessageAPI page with one of the enumerated messages.  See [Message]
         */
        fun createEnumMessageRedirect(msg: Message): PreparedResponseData {
            return ServerUtilities.redirectTo("$path?$enumeratedMessageKey=$msg")
        }

        /**
         * Redirects to the MessageAPI page with a custom message up to 200 charaters long.
         */
        fun createCustomMessageRedirect(msg: String, isSuccess: Boolean, returnPath: String): PreparedResponseData {
            require(msg.length <= 200)
            return ServerUtilities.redirectTo("$path?$customReturnLinkKey=${encode(returnPath)}&$customIsSuccessKey=${isSuccess.toString().toLowerCase()}&$customMessageKey=${encode(msg)}")
        }

    }

    /**
     * The general type of message, used by the page to display different graphic
     * styles based on the type - maybe for success, happy green and for failure dismal red... or something.
     */
    enum class MessageType {
        /**
         * Indicating something happened successfully, for example, successfully saving the log settings
         */
        SUCCESS,

        /**
         * Indicating something failed, for example, if someone tried registering
         * with a username that already existed
         */
        FAILURE
    }


    interface IMessage {
        val text: String
        val returnLink: String
        val type: MessageType
        val statusCode: StatusCode
    }

    class CustomMessage(override val text: String, override val returnLink: String, override val type: MessageType, override val statusCode: StatusCode) : IMessage

    enum class Message(override val text: String, override val returnLink: String, override val type: MessageType, override val statusCode: StatusCode) : IMessage {
        // if no value was provided for the message in the query string
        NO_MESSAGE("NO MESSAGE", HomepageAPI.path, MessageType.FAILURE, StatusCode.BAD_REQUEST),

        // if there was no matching message found using the key in the query string
        NO_MATCHING_MESSAGE("NO MATCHING MESSAGE", HomepageAPI.path, MessageType.FAILURE, StatusCode.BAD_REQUEST),

        // if we were sent both a custom message *and* an enumerated message
        MIXED_MESSAGE_TYPES("MIXED MESSAGE TYPES", HomepageAPI.path, MessageType.FAILURE, StatusCode.BAD_REQUEST),

        LOG_SETTINGS_SAVED("The log settings were saved", LoggingAPI.path, MessageType.SUCCESS, StatusCode.OK),
        FAILED_LOGIN("authentication failed", LoginAPI.path, MessageType.FAILURE, StatusCode.UNAUTHORIZED),
        FAILED_CREATE_PROJECT_DUPLICATE("duplicate project", ProjectAPI.path, MessageType.FAILURE, StatusCode.OK),
        FAILED_CREATE_EMPLOYEE_DUPLICATE("duplicate employee", CreateEmployeeAPI.path, MessageType.FAILURE, StatusCode.OK),
        INVALID_PROJECT_DURING_ENTERING_TIME("invalid project", ViewTimeAPI.path, MessageType.FAILURE, StatusCode.OK),
        PROJECT_DELETED("The project was deleted", ProjectAPI.path, MessageType.SUCCESS, StatusCode.OK),
        PROJECT_USED("The project was already used and therefore unable to be deleted", ProjectAPI.path, MessageType.FAILURE, StatusCode.OK),

        // messages for problems during entering time
        MINUTES_MUST_BE_MULTIPLE_OF_HALF_HOUR("The time entered must be on half-hour divisions", ViewTimeAPI.path, MessageType.FAILURE, StatusCode.OK),
        TIME_MUST_BE_LESS_OR_EQUAL_TO_24("The time entered must be less or equal to 24 hours", ViewTimeAPI.path, MessageType.FAILURE, StatusCode.OK),
        TOTAL_TIME_MUST_BE_LESS_OR_EQUAL_TO_24("The total time entered for a day must be less or equal to 24 hours", ViewTimeAPI.path, MessageType.FAILURE, StatusCode.OK),
        NO_TIME_ENTRY_ALLOWED_IN_SUBMITTED_PERIOD("It is not allowed to enter new time in a submitted period", ViewTimeAPI.path, MessageType.FAILURE, StatusCode.OK),

        // message for problems during editing time
        EDIT_MINUTES_MUST_BE_MULTIPLE_OF_15("The time entered must be on quarter-hour divisions", ViewTimeAPI.path, MessageType.FAILURE, StatusCode.OK),
        EDIT_TIME_MUST_BE_LESS_OR_EQUAL_TO_24("The time entered must be less or equal to 24 hours", ViewTimeAPI.path, MessageType.FAILURE, StatusCode.OK),
        EDIT_TOTAL_TIME_MUST_BE_LESS_OR_EQUAL_TO_24("The total time entered for a day must be less or equal to 24 hours", ViewTimeAPI.path, MessageType.FAILURE, StatusCode.OK),
        EDIT_NO_TIME_ENTRY_ALLOWED_IN_SUBMITTED_PERIOD("It is not allowed to enter new time in a submitted period", ViewTimeAPI.path, MessageType.FAILURE, StatusCode.OK),

        // messages for deleting employees
        EMPLOYEE_DELETED("Employee successfully deleted", CreateEmployeeAPI.path, MessageType.SUCCESS, StatusCode.OK),
        EMPLOYEE_USED("Unable to delete employee - already registered to a user", CreateEmployeeAPI.path, MessageType.FAILURE, StatusCode.OK),
        FAILED_TO_DELETE_EMPLOYEE("general failure to delete employee", CreateEmployeeAPI.path, MessageType.FAILURE, StatusCode.INTERNAL_SERVER_ERROR),
    }
}