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
val pageExtractorRegex = "GET /(.*) HTTP/1.1".toRegex()

class ServerUtilities(private val server: IOHolder) {

    /**
     * Serve prepared response object to the client
     */
    fun serverHandleRequest() {
        // read a line the client is sending us (the request,
        // per HTTP/1.1 protocol), e.g. GET /index.html HTTP/1.1
        val serverInput = server.readLine()
        val action = parseClientRequest(serverInput)
        val responseData = prepareDataForServing(action)
        returnData(responseData)
    }

    private fun prepareDataForServing(action: Action): PreparedResponseData {
        return when (action.type) {

            ResponseType.BAD_REQUEST -> {
                PreparedResponseData(FileReader.readNotNull("400error.html"), "400", "BAD REQUEST")
            }

            ResponseType.READ_FILE,
            ResponseType.TEMPLATE -> {
                val fileContents = FileReader.read(action.filename)
                if (fileContents == null) {
                    PreparedResponseData(FileReader.readNotNull("404error.html"), "404", "NOT FOUND")
                } else {
                    if (action.type == ResponseType.TEMPLATE) {
                        PreparedResponseData(renderTemplate(fileContents), "200", "OK")
                    } else {
                        PreparedResponseData(fileContents, "200", "OK")
                    }

                }
            }

        }
    }


    /**
     * Possible behaviors of the server
     */
    enum class ResponseType {
        /**
         * Just read a file, plain and simple
         */
        READ_FILE,

        /**
         * This file will require rendering
         */
        TEMPLATE,

        /**
         * The client sent us a bad (malformed) request
         */
        BAD_REQUEST,
    }

    /**
     * Encapsulates the proper action by the server, based on what
     * the client wants from us
     */
    data class Action(val type : ResponseType, val filename : String)

    /**
     * Based on the request from the client, come up with an [Action]
     * of what we should do next
     */
    private fun parseClientRequest(clientRequest : String) : Action {
        val result = pageExtractorRegex.matchEntire(clientRequest)
        val responseType : ResponseType
        var file = ""

        if (result == null) {
            responseType = ResponseType.BAD_REQUEST
        } else {
            // determine which file the client is requesting
            file = checkNotNull(result.groups[1]).value

            if (file.takeLast(4) == ".utl") {
                responseType = ResponseType.TEMPLATE
            } else {
                responseType = ResponseType.READ_FILE
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

}
