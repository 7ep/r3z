package coverosR3z.server

import coverosR3z.authentication.AuthenticationPersistence
import coverosR3z.authentication.AuthenticationUtilities
import coverosR3z.authentication.CurrentUser
import coverosR3z.domainobjects.*
import coverosR3z.logging.logDebug
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.templating.FileReader
import coverosR3z.templating.TemplatingEngine
import coverosR3z.timerecording.TimeEntryPersistence
import coverosR3z.timerecording.TimeRecordingUtilities
import java.lang.NumberFormatException
import java.time.LocalDate

/**
 * Data for shipping to the client
 */
data class PreparedResponseData(val fileContents: String, val responseStatus: ResponseStatus, val type: ContentType)

/**
 * These are mime types (see https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types)
 * which we'll use when conversing with clients to describe data
 */
enum class ContentType(val value: String) {

    // Text MIME types - see https://www.iana.org/assignments/media-types/media-types.xhtml#text
    TEXT_HTML("text/html"),
    TEXT_CSS("text/css"),

    // Application MIME types - see https://www.iana.org/assignments/media-types/media-types.xhtml#application
    APPLICATION_JAVASCRIPT("application/javascript"),
}

enum class ResponseStatus(val value: String) {
    OK("200 OK"),
    NOT_FOUND("404 NOT FOUND"),
    BAD_REQUEST("400 BAD REQUEST"),
}
/**
 * This is our regex for looking at a client's request
 * and determining what to send them.  For example,
 * if they send GET /sample.html HTTP/1.1, we send them sample.html
 *
 * On the other hand if it's not a well formed request, or
 * if we don't have that file, we reply with an error page
 */
val pageExtractorRegex = "(GET|POST) /(.*) HTTP/1.1".toRegex()

/**
 * Used for extracting the length of the body, in POSTs and
 * responses from servers
 */
val contentLengthRegex = "Content-Length: (.*)$".toRegex()

/**
 * Used to extract cookies from the Cookie header
 */
val cookieRegex = "Cookie: (.*)$".toRegex()

/**
 * Within a cookie header, we want our sessionId cookie, which
 * this regular expression will find
 */
val sessionIdCookieRegex = "sessionId=(.*)".toRegex()

class ServerUtilities(private val server: IOHolder, private val pmd : PureMemoryDatabase) {

    /**
     * Serve prepared response object to the client
     */
    fun serverHandleRequest() {
        // read a line the client is sending us (the request,
        // per HTTP/1.1 protocol), e.g. GET /index.html HTTP/1.1
        val serverInput = server.readLine()
        logDebug("request from client: $serverInput")

        val action: Action = parseClientRequest(serverInput)
        if (action.type == ActionType.HANDLE_POST_FROM_CLIENT) {
            handlePost(server) // a POST will have a body
        } else {
            handleGet(action) // there is nothing more to get for a GET
        }
    }

    /**
     * The user is asking us for something here
     */
    private fun handleGet(action: Action) {
        val responseData = prepareDataForServing(action)
        returnData(responseData)
    }

    /**
     * The user has sent us data, we have to process it
     */
    private fun handlePost(server : IOHolder) {
        val headers: List<String> = getHeaders(server)
        val length: Int = extractLengthOfPostBodyFromHeaders(headers)
        val authCookie : String? = extractAuthCookieFromHeaders(headers)
        val body = server.read(length)
        val data = parsePostedData(body)
        // enter the time *****
        //**************************************************************************
        //    D A N G E R    Z O N E - BEGINS
        //**************************************************************************

        val authPersistence = AuthenticationPersistence(pmd)
        val au = AuthenticationUtilities(authPersistence)

        val systemTru = TimeRecordingUtilities(TimeEntryPersistence(pmd), CurrentUser(SYSTEM_USER))
        val employee = systemTru.createEmployee(EmployeeName("Jona"))

        val password = "password1234567"
        val username = "jona"
        au.register(username, password, employee.id)
        val (_, user) = au.login(username, password)

        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd), CurrentUser(user))

        val project = checkNotNull(data["project_entry"])
        val createdProject = tru.createProject(ProjectName(project))

        val timeEntry = TimeEntryPreDatabase(employee,
                createdProject,
                Time(checkNotNull(data["time_entry"]).toInt()),
                Date(LocalDate.now().toEpochDay().toInt()),
                Details(checkNotNull(data["detail_entry"])))

        tru.recordTime(timeEntry)
        //**************************************************************************
        //    D A N G E R    Z O N E - ENDS
        //**************************************************************************

        returnData(PreparedResponseData("<p>Thank you, your time has been recorded</p>", ResponseStatus.OK, ContentType.TEXT_HTML))
    }

    /**
     * sends data as the body of a response from server
     */
    private fun returnData(data: PreparedResponseData) {
        logDebug("Assembling data just before shipping to client")
        val status = "HTTP/1.1 ${data.responseStatus.value}"
        logDebug("status: $status")
        val contentLengthHeader = "Content-Length: ${data.fileContents.length}"
        val contentTypeHeader = "Content-Type: ${data.type.value}"

        logDebug("contentLengthHeader: $contentLengthHeader")
        logDebug("contentTypeHeader: $contentTypeHeader")
        val input = "$status\n" +
                "$contentLengthHeader\n" +
                "$contentTypeHeader\n" +
                "\n" +
                data.fileContents
        server.write(input)
    }

    companion object {

        /**
         * Given the list of headers, find the one with the length of the
         * body of the POST and return that value as a simple [Int]
         */
        fun extractLengthOfPostBodyFromHeaders(headers: List<String>): Int {
            require(headers.isNotEmpty()) {"We received no headers"}
            try {
                val lengthHeader: String = headers.single { it.startsWith("Content-Length") }
                val length: Int? = contentLengthRegex.matchEntire(lengthHeader)?.groups?.get(1)?.value?.toInt()
                // arbitrarily setting to 500 for now
                val maxContentLength = 500
                checkNotNull(length) {"The length was null for this input: $lengthHeader"}
                check(length <= maxContentLength) {"Content-length was too large.  Maximum is $maxContentLength characters"}
                return length
            } catch (ex : NoSuchElementException) {
                throw NoSuchElementException("Did not find a necessary Content-Length header in headers. Headers: ${headers.joinToString(";")}")
            } catch (ex : NumberFormatException) {
                throw NumberFormatException("The value for content-length was not parsable as an integer. Headers: ${headers.joinToString(";")}")
            } catch (ex : Exception) {
                throw Exception("Exception occurred for these headers: ${headers.joinToString(";")}.  Inner exception message: ${ex.message}")
            }
        }

        /**
         * Given the list of headers, find the one with the authentication
         * cookie and return its value for further processing
         */
        fun extractAuthCookieFromHeaders(headers: List<String>): String? {
            if (headers.isEmpty()) return null

            return try {
                val cookieHeaders = headers.filter { it.startsWith("Cookie:") }
                val concatenatedHeaders = cookieHeaders.joinToString(";") { cookieRegex.matchEntire(it)?.groups?.get(1)?.value ?: "" }
                val splitUpCookies = concatenatedHeaders.split(";").map{it.trim()}
                val sessionCookies = splitUpCookies.mapNotNull { sessionIdCookieRegex.matchEntire(it)?.groups?.get(1)?.value }
                sessionCookies.singleOrNull()
            } catch (ex : NoSuchElementException) {
                throw NoSuchElementException("Did not find a necessary Cookie header in headers. Headers: ${headers.joinToString(";")}")
            }
        }

        /**
         * Parse data formatted by application/x-www-form-urlencoded
         * See https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST
         *
         * for example, valuea=3&valueb=this+is+something
         */
        fun parsePostedData(input: String): Map<String, String> {
            require(input.isNotEmpty()) {"The input to parse was empty"}
            // Need to split up '&' separated fields into keys and values and pack into a kotlin map
            // Closures for efficiency ahoy, sorry
            try {
                return (input.split("&").associate { field ->
                    field.split("=").let { it[0] to it[1].replace("+", " ") }
                })
            } catch (ex : IndexOutOfBoundsException) {
                throw IllegalArgumentException("We failed to parse \"$input\" as application/x-www-form-urlencoded", ex)
            }

        }

        /**
         * Helper method to collect the headers line by line, stopping when it
         * encounters an empty line
         */
        fun getHeaders(client: IOHolder): List<String> {
            // get the headers
            val headers = mutableListOf<String>()
            while (true) {
                val header = client.readLine()
                if (header.isNotEmpty()) {
                    headers.add(header)
                } else {
                    break
                }
            }
            return headers
        }

        private fun prepareDataForServing(action: Action): PreparedResponseData {
            return when (action.type) {
                ActionType.BAD_REQUEST -> handleBadRequest()

                ActionType.READ_FILE,
                ActionType.CSS,
                ActionType.JS,
                ActionType.TEMPLATE -> handleReadingFiles(action)

                ActionType.HANDLE_POST_FROM_CLIENT ->  TODO("Not yet implemented")
            }
        }

        private fun handleBadRequest() = PreparedResponseData(FileReader.readNotNull("400error.html"), ResponseStatus.BAD_REQUEST, ContentType.TEXT_HTML)

        private fun handleReadingFiles(action: Action): PreparedResponseData {
            val fileContents = FileReader.read(action.filename)
            return if (fileContents == null) {
                logDebug("unable to read a file named ${action.filename}")
                val unfound = FileReader.readNotNull("404error.html")
                PreparedResponseData(unfound, ResponseStatus.NOT_FOUND, ContentType.TEXT_HTML)
            } else {
                if (action.type == ActionType.TEMPLATE) {
                    logDebug("Sending file for rendering")
                    val renderedFile = renderTemplate(fileContents)
                    PreparedResponseData(renderedFile,ResponseStatus.OK, ContentType.TEXT_HTML)
                } else {
                    when (action.type) {
                        ActionType.CSS -> PreparedResponseData(fileContents, ResponseStatus.OK, ContentType.TEXT_CSS)
                        ActionType.JS -> PreparedResponseData(fileContents, ResponseStatus.OK, ContentType.APPLICATION_JAVASCRIPT)
                        else -> PreparedResponseData(fileContents, ResponseStatus.OK, ContentType.TEXT_HTML)
                    }
                }

            }
        }


        /**
         * Possible behaviors of the server
         */
        enum class ActionType {
            /**
             * Just read a file, plain and simple
             */
            READ_FILE,

            /**
             * This file will require rendering
             */
            TEMPLATE,

            /**
             * The server has sent us data with a post.
             * We have to handle it before we respond
             */
            HANDLE_POST_FROM_CLIENT,

            /**
             * The client sent us a bad (malformed) request
             */
            BAD_REQUEST,

            /**
             * Cascading style sheet
             */
            CSS,

            /**
             * A JavaScript file
             */
            JS,
        }

        /**
         * Encapsulates the proper action by the server, based on what
         * the client wants from us
         */
        data class Action(val type: ActionType, val filename: String = "", val data : Map<String, String> = emptyMap())

        /**
         * Based on the request from the client, come up with an [Action]
         * of what we should do next
         */
        fun parseClientRequest(clientRequest: String): Action {
            val result = pageExtractorRegex.matchEntire(clientRequest)
            val responseType: ActionType
            var file = ""

            if (result == null) {
                logDebug("Unable to parse client request")
                responseType = ActionType.BAD_REQUEST
            } else {
                // determine which file the client is requesting
                val verb = checkNotNull(result.groups[1]).value
                logDebug("verb from client was: $verb")

                if (verb == "POST") {
                    logDebug("Handling POST from client")
                    responseType = ActionType.HANDLE_POST_FROM_CLIENT

                } else {
                    logDebug("Handling GET from client")
                    file = checkNotNull(result.groups[2]).value
                    logDebug("Client wants this file: $file")

                    if (file.takeLast(4) == ".utl") {
                        logDebug("file requested is a template")
                        responseType = ActionType.TEMPLATE
                    } else if (file.takeLast(4) == ".css") {
                        logDebug("file requested is a CSS style sheet")
                        responseType = ActionType.CSS
                    } else if (file.takeLast(3) == ".js") {
                        logDebug("file requested is a JavaScript file")
                        responseType = ActionType.JS
                    } else {
                        logDebug("file requested is a text file")
                        responseType = ActionType.READ_FILE
                    }
                }
            }

            return Action(responseType, file)
        }

        private fun renderTemplate(template: String): String {
            val te = TemplatingEngine()
            // TODO: replace following code ASAP
            val mapping = mapOf("username" to "Jona")
            return te.render(template, mapping)
        }

    }

}
