package coverosR3z.server

import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.domainobjects.*
import coverosR3z.logging.logDebug
import coverosR3z.logging.logInfo
import coverosR3z.server.NamedPaths.*
import coverosR3z.templating.FileReader
import coverosR3z.templating.TemplatingEngine
import coverosR3z.timerecording.ITimeRecordingUtilities

class ServerUtilities(private val au: IAuthenticationUtilities,
                      private val tru: ITimeRecordingUtilities) {

    val isAuthenticated = {rd : RequestData -> rd.user != NO_USER}

    /**
     * When you just want to simply read an ordinary file
     */
    private fun simpleRead(path : String) =
            PreparedResponseData(FileReader.readNotNull(path), ResponseStatus.OK)

    /**
     * Examine the request and take proper action, returning a
     * proper response
     */
    fun handleRequestAndRespond(requestData: RequestData): PreparedResponseData {
        return when (requestData.verb) {
            Verb.POST -> handlePost(requestData)
            Verb.GET -> handleGet(requestData)
            Verb.INVALID -> handleBadRequest()
        }
    }

    private fun handleGet(rd: RequestData): PreparedResponseData {
        return when (rd.path){
            "", HOMEPAGE.value -> simpleRead(HOMEPAGE.assocFile)
            ENTER_TIME.value -> doGETEnterTimePage(rd)
            CREATE_EMPLOYEE.value -> doGETCreateEmployeePage(rd)
            LOGIN.value -> doGETLoginPage(rd)
            REGISTER.value -> doGETRegisterPage(rd)
            CREATE_PROJECT.value -> doGETCreateProjectPage(rd)
            else -> {
                val fileContents = FileReader.read(rd.path)
                if (fileContents == null) {
                    logDebug("unable to read a file named ${rd.path}")
                    handleNotFound()
                } else when {
                    rd.path.takeLast(4) == ".css" -> PreparedResponseData(fileContents, ResponseStatus.OK, listOf(ContentType.TEXT_CSS.ct))
                    rd.path.takeLast(3) == ".js" -> PreparedResponseData(fileContents, ResponseStatus.OK, listOf(ContentType.APPLICATION_JAVASCRIPT.ct))
                    else -> handleNotFound()
                }
            }
        }
    }

    /**
     * The user has sent us data, we have to process it
     */
    fun handlePost(rd: RequestData) : PreparedResponseData {
        return when (rd.path) {
            ENTER_TIME.value -> handlePOSTTimeEntry(rd.user, rd.data)
            CREATE_EMPLOYEE.value -> handlePOSTNewEmployee(rd.user, rd.data)
            LOGIN.value -> handlePOSTLogin(rd.user, rd.data)
            REGISTER.value -> handlePOSTRegister(rd.user, rd.data)
            CREATE_PROJECT.value -> handlePOSTCreatingProject(rd.user, rd.data)
            else -> handleNotFound()
        }
    }

    private fun doGETCreateProjectPage(rd: RequestData): PreparedResponseData {
        return if (isAuthenticated(rd)) {
            simpleRead(CREATE_PROJECT.assocFile)
        } else {
            redirectTo(HOMEPAGE.value)
        }
    }

    private fun doGETCreateEmployeePage(rd: RequestData): PreparedResponseData {
        return if (isAuthenticated(rd)) {
            val contents = FileReader.readNotNull(CREATE_EMPLOYEE.assocFile)
            val te = TemplatingEngine()
            val mapping = mapOf("username" to rd.user.name)
            val rendered = te.render(contents, mapping)
            PreparedResponseData(rendered, ResponseStatus.OK)
        } else {
            redirectTo(HOMEPAGE.value)
        }
    }

    private fun doGETLoginPage(rd: RequestData): PreparedResponseData {
        return if (isAuthenticated(rd)) {
            redirectTo(AUTHHOMEPAGE.value)
        } else {
            simpleRead(LOGIN.assocFile)
        }
    }

    private fun doGETEnterTimePage(rd : RequestData): PreparedResponseData {
        return if (isAuthenticated(rd)) {
            val contents = FileReader.readNotNull(ENTER_TIME.assocFile)
            val te = TemplatingEngine()
            val mapping = mapOf("username" to rd.user.name)
            val rendered = te.render(contents, mapping)
            PreparedResponseData(rendered, ResponseStatus.OK)
        } else {
            redirectTo(ENTER_TIME.value)
        }
    }

    private fun handleBadRequest(): PreparedResponseData {
        return PreparedResponseData(FileReader.readNotNull(BAD_REQUEST.assocFile), ResponseStatus.BAD_REQUEST, listOf(ContentType.TEXT_HTML.ct))
    }

    private fun handleNotFound(): PreparedResponseData {
        return PreparedResponseData(FileReader.readNotNull(NOT_FOUND.assocFile), ResponseStatus.NOT_FOUND, listOf(ContentType.TEXT_HTML.ct))
    }

    private fun handleUnauthorized() : PreparedResponseData {
        return PreparedResponseData(FileReader.readNotNull(UNAUTHORIZED.assocFile), ResponseStatus.UNAUTHORIZED, listOf(ContentType.TEXT_HTML.ct))
    }

    private fun handlePOSTCreatingProject(user: User, data: Map<String, String>) : PreparedResponseData {
        val isAuthenticated = user != NO_USER
        return if (isAuthenticated) {
            tru.createProject(ProjectName(checkNotNull(data["projectname"])))
            PreparedResponseData(FileReader.readNotNull("success.html"), ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct))
        } else {
            handleUnauthorized()
        }
    }

    private fun doGETRegisterPage(rd: RequestData): PreparedResponseData {
        return if (isAuthenticated(rd)) {
            redirectTo(AUTHHOMEPAGE.value)
        } else {
            simpleRead(REGISTER.assocFile)
        }
    }

    private fun handlePOSTRegister(user: User, data: Map<String, String>) : PreparedResponseData {
        return if (user == NO_USER) {
            val username = checkNotNull(data["username"])
            val password = checkNotNull(data["password"])
            val result = au.register(username, password)
            if (result == RegistrationResult.SUCCESS) {
                PreparedResponseData(FileReader.readNotNull("success.html"), ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct))
            } else {
                PreparedResponseData(FileReader.readNotNull("failure.html"), ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct))
            }
        } else {
            redirectTo(AUTHHOMEPAGE.value)
        }
    }

    private fun handlePOSTLogin(user: User, data: Map<String, String>) : PreparedResponseData {
        val isUnauthenticated = user == NO_USER
        return if (isUnauthenticated) {
            val username = checkNotNull(data["username"])
            val password = checkNotNull(data["password"])
            val (loginResult, loginUser) = au.login(username, password)
            if (loginResult == LoginResult.SUCCESS && loginUser != NO_USER) {
                val newSessionToken: String = au.createNewSession(loginUser)
                PreparedResponseData(FileReader.readNotNull("success.html"), ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct, "Set-Cookie: sessionId=$newSessionToken"))
            } else {
                logInfo("User ($username) failed to login")
                handleUnauthorized()
            }
        } else {
            redirectTo(AUTHHOMEPAGE.value)
        }
    }

    /**
     * Use this to redirect to any particular page
     */
    private fun redirectTo(path: String): PreparedResponseData {
        return PreparedResponseData("", ResponseStatus.SEE_OTHER, listOf(ContentType.TEXT_HTML.ct, "Location: $path"))
    }

    private fun handlePOSTNewEmployee(user: User, data: Map<String, String>) : PreparedResponseData {
        val isAuthenticated = user != NO_USER
        return if (isAuthenticated) {
            tru.createEmployee(EmployeeName(checkNotNull(data["employee_name"])))
            PreparedResponseData(FileReader.readNotNull("success.html"), ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct))
        } else {
            handleUnauthorized()
        }
    }

    private fun handlePOSTTimeEntry(user: User, data: Map<String, String>) : PreparedResponseData {
        val isAuthenticated = user != NO_USER
        return if (isAuthenticated) {
            PreparedResponseData(FileReader.readNotNull("success.html"), ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct))
        } else {
            handleUnauthorized()
        }
    }

    companion object {

        /**
         * This is our regex for looking at a client's request
         * and determining what to send them.  For example,
         * if they send GET /sample.html HTTP/1.1, we send them sample.html
         *
         * On the other hand if it's not a well formed request, or
         * if we don't have that file, we reply with an error page
         */
        private val pageExtractorRegex = "(GET|POST) /(.*) HTTP/1.1|1.0".toRegex()

        /**
         * Used for extracting the length of the body, in POSTs and
         * responses from servers
         */
        val contentLengthRegex = "Content-Length: (.*)$".toRegex()

        /**
         * Used to extract cookies from the Cookie header
         */
        private val cookieRegex = "Cookie: (.*)$".toRegex()

        /**
         * Within a cookie header, we want our sessionId cookie, which
         * this regular expression will find
         */
        private val sessionIdCookieRegex = "sessionId=(.*)".toRegex()

        /**
         * Based on the request from the client, come up with an [RequestData]
         * of what we should do next
         */
        fun parseClientRequest(server: ISocketWrapper, au: IAuthenticationUtilities): RequestData {
            // read the first line for the fundamental request
            val clientRequest = server.readLine()
            val (verb, path) = parseFirstLine(clientRequest)

            val headers = getHeaders(server)

            val token = extractSessionTokenFromHeaders(headers)
            val user = extractUserFromAuthToken(token, au)
            val data = extractDataIfPost(server,verb, headers)

            return RequestData(verb, path, data, user)
        }

        /**
         * read the body if one exists and convert it to a string -> string map
         */
        private fun extractDataIfPost(server: ISocketWrapper, verb : Verb,  headers: List<String>): Map<String, String> {
            return if (verb == Verb.POST) {
                val length = extractLengthOfPostBodyFromHeaders(headers)
                val body = server.read(length)
                parsePostedData(body)
            } else {
                emptyMap()
            }
        }

        /**
         * Given the auth token extracted from a cookie,
         * return the user it represents, but only if it
         * represents a current valid session.
         *
         * returns [NO_USER] otherwise
         */
        fun extractUserFromAuthToken(authCookie: String?, au: IAuthenticationUtilities): User {
            return if (authCookie.isNullOrBlank()) {
                NO_USER
            } else {
                au.getUserForSession(authCookie)
            }
        }

        /**
         * The first line of the request is the basic request
         * from the client.  See [pageExtractorRegex]
         */
        fun parseFirstLine(clientRequest: String): Pair<Verb, String> {
            logDebug("request from client: $clientRequest")
            val result = pageExtractorRegex.matchEntire(clientRequest)
            val verb: Verb
            var file = ""

            if (result == null) {
                logDebug("Unable to parse client request")
                verb = Verb.INVALID
            } else {
                // determine which file the client is requesting
                verb = Verb.valueOf(checkNotNull(result.groups[1]).value)
                logDebug("verb from client was: $verb")
                file = checkNotNull(result.groups[2]).value
                logDebug("path from client was: $file")
            }

            return Pair(verb, file)
        }

        /**
         * Given the list of headers, find the one with the length of the
         * body of the POST and return that value as a simple [Int]
         */
        fun extractLengthOfPostBodyFromHeaders(headers: List<String>): Int {
            require(headers.isNotEmpty()) {"We must receive at least one header at this point or the request is invalid"}
            try {
                val lengthHeader: String = headers.single { it.startsWith("Content-Length") }
                val length: Int? = contentLengthRegex.matchEntire(lengthHeader)?.groups?.get(1)?.value?.toInt()
                // arbitrarily setting to 500 for now
                val maxContentLength = 500
                checkNotNull(length) {"The length must not be null for this input.  It was: $lengthHeader"}
                check(length <= maxContentLength) {"The Content-length is not allowed to exceed $maxContentLength characters"}
                check(length >= 0) {"Content-length cannot be negative"}
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
         *
         * This value corresponds to the identifier for a session in the database
         */
        fun extractSessionTokenFromHeaders(headers: List<String>): String? {
            if (headers.isEmpty()) return null

            val cookieHeaders = headers.filter { it.startsWith("Cookie:") }
            val concatenatedHeaders = cookieHeaders.joinToString(";") { cookieRegex.matchEntire(it)?.groups?.get(1)?.value ?: "" }
            val splitUpCookies = concatenatedHeaders.split(";").map{it.trim()}
            val sessionCookies = splitUpCookies.mapNotNull { sessionIdCookieRegex.matchEntire(it)?.groups?.get(1)?.value }
            return sessionCookies.singleOrNull()
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
        fun getHeaders(socket: ISocketWrapper): List<String> {
            // get the headers
            val headers = mutableListOf<String>()
            while (true) {
                val header = socket.readLine()
                if (header.isNotEmpty()) {
                    headers.add(header)
                } else {
                    break
                }
            }
            return headers
        }

        /**
         * sends data as the body of a response from server
         */
        fun returnData(server: ISocketWrapper, data: PreparedResponseData) {
            logDebug("Assembling data just before shipping to client")
            val status = "HTTP/1.1 ${data.responseStatus.value}"
            logDebug("status: $status")
            val contentLengthHeader = "Content-Length: ${data.fileContents.length}"
            val otherHeaders = data.headers.joinToString("\n") + "\n"
            logDebug("contentLengthHeader: $contentLengthHeader")
            logDebug("other headers:\n $otherHeaders")
            val input = "$status\n" +
                    "$contentLengthHeader\n" +
                    otherHeaders +
                    "\n" +
                    data.fileContents
            server.write(input)
        }
    }

}
