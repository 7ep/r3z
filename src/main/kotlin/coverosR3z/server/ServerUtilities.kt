package coverosR3z.server

import coverosR3z.templating.FileReader
import coverosR3z.templating.TemplatingEngine

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

class ServerUtilities(private val server: IOHolder) {

    /**
     * Serve prepared response object to the client
     */
    fun serverHandleRequest() {
        // read a line the client is sending us (the request,
        // per HTTP/1.1 protocol), e.g. GET /index.html HTTP/1.1
        val serverInput = server.readLine()
        val action: Action = parseClientRequest(serverInput)
        if (action.type == ActionType.HANDLE_POST_FROM_CLIENT) {
            handlePost(server)
        } else {
            handleGet(action)
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
        val body = server.readLine()
        val data = parsePostedData(body)
        // respond, "thanks, your time has been entered"
    }

    /**
     * sends data as the body of a response from server
     */
    private fun returnData(data: PreparedResponseData) {
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

        private fun handlePostFromClient(action : Action) {
            val foo = action.data
        }

        private fun handleBadRequest() = PreparedResponseData(FileReader.readNotNull("400error.html"), "400", "BAD REQUEST")

        private fun handleReadingFiles(action: Action): PreparedResponseData {
            val fileContents = FileReader.read(action.filename)
            return if (fileContents == null) {
                PreparedResponseData(FileReader.readNotNull("404error.html"), "404", "NOT FOUND")
            } else {
                if (action.type == ActionType.TEMPLATE) {
                    PreparedResponseData(renderTemplate(fileContents), "200", "OK")
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
                responseType = ActionType.BAD_REQUEST
            } else {
                // determine which file the client is requesting
                val verb = checkNotNull(result.groups[1]).value

                if (verb == "POST") {
                    responseType = ActionType.HANDLE_POST_FROM_CLIENT

                } else {
                    file = checkNotNull(result.groups[2]).value

                    if (file.takeLast(4) == ".utl") {
                        responseType = ActionType.TEMPLATE
                    } else {
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
