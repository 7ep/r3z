package coverosR3z.server

import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.authentication.doGETRegisterPage
import coverosR3z.authentication.handlePOSTRegister
import coverosR3z.domainobjects.*
import coverosR3z.logging.logDebug
import coverosR3z.logging.logInfo
import coverosR3z.misc.FileReader
import coverosR3z.server.NamedPaths.*
import coverosR3z.timerecording.ITimeRecordingUtilities
import coverosR3z.webcontent.*
import java.time.LocalDate


class ServerUtilities(private val au: IAuthenticationUtilities,
                      private val tru: ITimeRecordingUtilities) {

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
            "", HOMEPAGE.path -> doGetHomePage(rd)
            ENTER_TIME.path -> doGETEnterTimePage(rd)
            ENTER_TIMEJS.path -> okJS(enterTimeJS)
            ENTER_TIMECSS.path -> okCSS(enterTimeCSS)
            TIMEENTRIES.path -> doGetTimeEntriesPage(rd)
            CREATE_EMPLOYEE.path -> doGETCreateEmployeePage(rd)
            EMPLOYEES.path -> okHTML(existingEmployeesHTML(rd.user.name, tru.listAllEmployees()))
            LOGIN.path -> doGETLoginPage(rd)
            REGISTER.path -> doGETRegisterPage(tru, rd)
            REGISTERCSS.path -> okCSS(registerCSS)
            CREATE_PROJECT.path -> doGETCreateProjectPage(rd)
            LOGOUT.path -> doGETLogout(rd)
            else -> {
                val fileContents = FileReader.read(rd.path)
                if (fileContents == null) {
                    logDebug("unable to read a file named ${rd.path}")
                    handleNotFound()
                } else when {
                    rd.path.takeLast(4) == ".css" -> okCSS(fileContents)
                    rd.path.takeLast(3) == ".js" -> okJS(fileContents)
                    else -> handleNotFound()
                }
            }
        }
    }

    private fun doGetTimeEntriesPage(rd: RequestData): PreparedResponseData {
        return if (isAuthenticated(rd)) {
            okHTML(existingTimeEntriesHTML(rd.user.name, tru.getAllEntriesForEmployee(rd.user.employeeId ?: NO_EMPLOYEE.id)))
        } else {
            redirectTo(HOMEPAGE.path)
        }
    }

    /**
     * The user has sent us data, we have to process it
     */
    private fun handlePost(rd: RequestData) : PreparedResponseData {
        return when (rd.path) {
            ENTER_TIME.path -> handlePOSTTimeEntry(rd.user, rd.data)
            CREATE_EMPLOYEE.path -> handlePOSTNewEmployee(rd.user, rd.data)
            LOGIN.path -> handlePOSTLogin(rd.user, rd.data)
            REGISTER.path -> handlePOSTRegister(au, rd.user, rd.data)
            CREATE_PROJECT.path -> handlePOSTCreatingProject(rd.user, rd.data)
            else -> handleNotFound()
        }
    }

    private fun doGETLogout(rd: RequestData): PreparedResponseData {
        return if (isAuthenticated(rd)) {
            au.logout(rd.sessionToken)
            PreparedResponseData(logoutHTML, ResponseStatus.OK, emptyList())
        } else {
            redirectTo(HOMEPAGE.path)
        }
    }

    private fun doGETCreateProjectPage(rd: RequestData): PreparedResponseData {
        return if (isAuthenticated(rd)) {
            okHTML(createProjectHTML(rd.user.name))
        } else {
            redirectTo(HOMEPAGE.path)
        }
    }


    private fun doGetHomePage(rd: RequestData): PreparedResponseData {
        return if (isAuthenticated(rd)) {
            okHTML(authHomePageHTML(rd.user.name))
        } else {
            okHTML(homepageHTML)
        }
    }

    private fun doGETCreateEmployeePage(rd: RequestData): PreparedResponseData {
        return if (isAuthenticated(rd)) {
            okHTML(createEmployeeHTML(rd.user.name))
        } else {
            redirectTo(HOMEPAGE.path)
        }
    }

    private fun doGETLoginPage(rd: RequestData): PreparedResponseData {
        return if (isAuthenticated(rd)) {
            redirectTo(AUTHHOMEPAGE.path)
        } else {
            okHTML(loginHTML)
        }
    }

    private fun doGETEnterTimePage(rd : RequestData): PreparedResponseData {
        return if (isAuthenticated(rd)) {
            okHTML(entertimeHTML(rd.user.name, tru.listAllProjects()))
        } else {
            redirectTo(HOMEPAGE.path)
        }
    }

    private fun handleBadRequest(): PreparedResponseData {
        return PreparedResponseData(badRequestHTML, ResponseStatus.BAD_REQUEST, listOf(ContentType.TEXT_HTML.ct))
    }

    private fun handleNotFound(): PreparedResponseData {
        return PreparedResponseData(notFoundHTML, ResponseStatus.NOT_FOUND, listOf(ContentType.TEXT_HTML.ct))
    }

    private fun handleUnauthorized() : PreparedResponseData {
        return PreparedResponseData(unauthorizedHTML, ResponseStatus.UNAUTHORIZED, listOf(ContentType.TEXT_HTML.ct))
    }

    private fun handlePOSTCreatingProject(user: User, data: Map<String, String>) : PreparedResponseData {
        val isAuthenticated = user != NO_USER
        return if (isAuthenticated) {
            tru.createProject(ProjectName(checkNotNull(data["project_name"]){"project_name must not be missing"}))
            PreparedResponseData(successHTML, ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct))
        } else {
            handleUnauthorized()
        }
    }




    private fun handlePOSTLogin(user: User, data: Map<String, String>) : PreparedResponseData {
        val isUnauthenticated = user == NO_USER
        return if (isUnauthenticated) {
            val username = checkNotNull(data["username"]) {"username must not be missing"}
            val password = checkNotNull(data["password"]) {"password must not be missing"}
            val (loginResult, loginUser) = au.login(username, password)
            if (loginResult == LoginResult.SUCCESS && loginUser != NO_USER) {
                val newSessionToken: String = au.createNewSession(loginUser)
                PreparedResponseData(successHTML, ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct, "Set-Cookie: sessionId=$newSessionToken"))
            } else {
                logInfo("User ($username) failed to login")
                handleUnauthorized()
            }
        } else {
            redirectTo(AUTHHOMEPAGE.path)
        }
    }

    private fun handlePOSTNewEmployee(user: User, data: Map<String, String>) : PreparedResponseData {
        val isAuthenticated = user != NO_USER
        return if (isAuthenticated) {
            tru.createEmployee(EmployeeName(checkNotNull(data["employee_name"]){"The employee_name must not be missing"}))
            PreparedResponseData(successHTML, ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct))
        } else {
            handleUnauthorized()
        }
    }

    private fun handlePOSTTimeEntry(user: User, data: Map<String, String>) : PreparedResponseData {
        val isAuthenticated = user != NO_USER
        return if (isAuthenticated) {
            val projectId = checkNotNull(data["project_entry"]){"project_entry must not be null"}.toInt()
            val time = Time(checkNotNull(data["time_entry"]){"time_entry must not be null"}.toInt())
            val details = Details(checkNotNull(data["detail_entry"]){"detail_entry must not be null"})

            val project = tru.findProjectById(projectId)
            val employee = tru.findEmployeeById(checkNotNull(user.employeeId){"employeeId must not be null"})

            val timeEntry = TimeEntryPreDatabase(
                    employee,
                    project,
                    time,
                    Date(LocalDate.now().toEpochDay().toInt()),
                    details)

            tru.recordTime(timeEntry)

            PreparedResponseData(successHTML, ResponseStatus.OK, listOf(ContentType.TEXT_HTML.ct))
        } else {
            handleUnauthorized()
        }
    }

    companion object {

        val isAuthenticated = {rd : RequestData -> rd.user != NO_USER}

        /**
         * If you are responding with a success message and it is HTML
         */
        fun okHTML(contents : String) =
                ok(contents, listOf(ContentType.TEXT_HTML.ct))
        /**
         * If you are responding with a success message and it is CSS
         */
        fun okCSS(contents : String) =
                ok(contents, listOf(ContentType.TEXT_CSS.ct))
        /**
         * If you are responding with a success message and it is JavaScript
         */
        fun okJS (contents : String) =
                ok(contents, listOf(ContentType.APPLICATION_JAVASCRIPT.ct))

        private fun ok (contents: String, ct : List<String>) =
                PreparedResponseData(contents, ResponseStatus.OK, ct)

        /**
         * Use this to redirect to any particular page
         */
        fun redirectTo(path: String): PreparedResponseData {
            return PreparedResponseData("", ResponseStatus.SEE_OTHER, listOf(ContentType.TEXT_HTML.ct, "Location: $path"))
        }

        /**
         * This is our regex for looking at a client's request
         * and determining what to send them.  For example,
         * if they send GET /sample.html HTTP/1.1, we send them sample.html
         *
         * On the other hand if it's not a well formed request, or
         * if we don't have that file, we reply with an error page
         */
        private val pageExtractorRegex = "(GET|POST) /(.*) HTTP/(?:1.1|1.0)".toRegex()

        /**
         * Used for extracting the length of the body, in POSTs and
         * responses from servers
         */
        val contentLengthRegex = "[cC]ontent-[lL]ength: (.*)$".toRegex()

        /**
         * Used to extract cookies from the Cookie header
         */
        private val cookieRegex = "[cC]ookie: (.*)$".toRegex()

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

            val token = extractSessionTokenFromHeaders(headers) ?: ""
            val user = extractUserFromAuthToken(token, au)
            val data = extractDataIfPost(server,verb, headers)

            return RequestData(verb, path, data, user, token)
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
                verb = Verb.valueOf(checkNotNull(result.groups[1]){"The HTTP verb must not be missing"}.value)
                logDebug("verb from client was: $verb")
                file = checkNotNull(result.groups[2]){"The requested path must not be missing"}.value
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
                val lengthHeader: String = headers.single { it.toLowerCase().startsWith("content-length") }
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

            val cookieHeaders = headers.filter { it.toLowerCase().startsWith("cookie:") }
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
