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
import java.time.LocalDate

/**
 * Data for shipping to the client
 */
data class PreparedResponseData(val fileContents: String, val code: String, val status: String)

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
        val headers: List<String> = getHeaders(server) // we may also use this, eventually
        val lengthHeader: String = headers.single { it.startsWith("Content-Length") }
        // TODO following should be made safe / clean
        val length: Int = contentLengthRegex.matchEntire(lengthHeader)!!.groups[1]!!.value.toInt()
        val body = server.read(length)
        val data = parsePostedData(body) // we may use this, eventually
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

        returnData(PreparedResponseData("<p>Thank you, your time has been recorded</p>", "200", "OK"))
    }

    /**
     * sends data as the body of a response from server
     */
    private fun returnData(data: PreparedResponseData) {
        logDebug("Assembling data just before shipping to client")
        val status = "HTTP/1.1 ${data.code} ${data.status}"
        val header = "Content-Length: ${data.fileContents.length}"
        val input = "$status\n" +
                "$header\n" +
                "\n" +
                data.fileContents
        server.write(input)
    }

    companion object {

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
                ActionType.TEMPLATE -> handleReadingFiles(action)

                ActionType.HANDLE_POST_FROM_CLIENT ->  TODO("Not yet implemented")
            }
        }

        private fun handleBadRequest() = PreparedResponseData(FileReader.readNotNull("400error.html"), "400", "BAD REQUEST")

        private fun handleReadingFiles(action: Action): PreparedResponseData {
            val fileContents = FileReader.read(action.filename)
            return if (fileContents == null) {
                logDebug("unable to read a file named ${action.filename}")
                val unfound = FileReader.readNotNull("404error.html")
                PreparedResponseData(unfound, "404", "NOT FOUND")
            } else {
                if (action.type == ActionType.TEMPLATE) {
                    logDebug("Sending file for rendering")
                    val renderedFile = renderTemplate(fileContents)
                    PreparedResponseData(renderedFile, "200", "OK")
                } else {
                    PreparedResponseData(fileContents, "200", "OK")
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
