package coverosR3z.server.api

import coverosR3z.authentication.api.LoginAPI
import coverosR3z.server.types.*
import coverosR3z.server.utility.PageComponents
import coverosR3z.server.utility.ServerUtilities
import coverosR3z.system.logging.LoggingAPI
import coverosR3z.timerecording.api.CreateEmployeeAPI
import coverosR3z.timerecording.api.ProjectAPI
import coverosR3z.timerecording.api.ViewTimeAPI
import java.lang.IllegalStateException

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
            val messageCode = extractMessageCode(sd)
            val body = renderBody(messageCode)
            val fullTemplate = PageComponents(sd).makeTemplate(
                title = messageCode.message,
                "MessageAPI",
                body = body,
                """<link rel="stylesheet" href="message.css" />""")
            return PreparedResponseData(fullTemplate, messageCode.statusCode, listOf(ContentType.TEXT_HTML.value))
        }

        /**
         * Code for rendering a message
         */
        private fun renderBody(messageCode: Message): String {
            return """
                <div class="container">
                    <div id="message_text" class="${messageCode.type.toString().toLowerCase()}" >${messageCode.message}</div>
                    <a class="button" href="${messageCode.returnLink}">OK</a>
                </div>
                    """.trimIndent()
        }

        /**
         * The bits and pieces of checking the query string for the expected
         * value and handling cases where it isn't there or isn't proper.
         */
        private fun extractMessageCode(sd: ServerData): Message {
            val messageCodeString = checkNotNull(
                sd.ahd.queryString[queryStringKey]
            ) { "This requires a message code - was null" }
            return try {
                Message.valueOf(messageCodeString)
            } catch (ex: Throwable) {
                throw IllegalStateException("No matching message code was provided")
            }
        }

        override val path: String = "result"
        const val queryStringKey: String = "msg"


        fun createMessageRedirect(msg: Message) =
            ServerUtilities.redirectTo("$path?$queryStringKey=$msg")

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


    enum class Message(val message: String, val returnLink: String, val type: MessageType, val statusCode: StatusCode) {
        LOG_SETTINGS_SAVED("The log settings were saved", LoggingAPI.path, MessageType.SUCCESS, StatusCode.OK),
        FAILED_LOGIN("authentication failed", LoginAPI.path, MessageType.FAILURE, StatusCode.UNAUTHORIZED),
        FAILED_CREATE_PROJECT_DUPLICATE("duplicate project", ProjectAPI.path, MessageType.FAILURE, StatusCode.OK),
        FAILED_CREATE_EMPLOYEE_DUPLICATE("duplicate employee", CreateEmployeeAPI.path, MessageType.FAILURE, StatusCode.OK),
        INVALID_PROJECT_DURING_ENTERING_TIME("invalid project", ViewTimeAPI.path, MessageType.FAILURE, StatusCode.OK),
        PROJECT_DELETED("The project was deleted", ProjectAPI.path, MessageType.SUCCESS, StatusCode.OK),
        PROJECT_USED("The project was already used and therefore unable to be deleted", ProjectAPI.path, MessageType.FAILURE, StatusCode.OK),
    }
}