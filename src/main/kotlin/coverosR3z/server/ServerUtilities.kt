package coverosR3z.server

import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.authentication.LoginAPI
import coverosR3z.authentication.RegisterAPI
import coverosR3z.authentication.generateLogoutPage
import coverosR3z.domainobjects.NO_USER
import coverosR3z.domainobjects.User
import coverosR3z.exceptions.DuplicateInputsException
import coverosR3z.logging.LoggingAPI
import coverosR3z.logging.logDebug
import coverosR3z.logging.logTrace
import coverosR3z.misc.*
import coverosR3z.server.HttpResponseCache.staticFileCache
import coverosR3z.server.NamedPaths.*
import coverosR3z.timerecording.EmployeeAPI
import coverosR3z.timerecording.EnterTimeAPI
import coverosR3z.timerecording.ITimeRecordingUtilities
import coverosR3z.timerecording.ProjectAPI

data class ServerData(val au: IAuthenticationUtilities, val tru: ITimeRecordingUtilities, val rd: AnalyzedHttpData)

/**
 *   HTTP/1.1 defines the sequence CR LF as the end-of-line marker for all
 *  protocol elements except the entity-body (see appendix 19.3 for
 *  tolerant applications). The end-of-line marker within an entity-body
 *  is defined by its associated media type, as described in section 3.7.
 *
 *  See https://tools.ietf.org/html/rfc2616
 */
const val CRLF = "\r\n"
const val CONTENT_LENGTH = "content-length"
const val maxContentLength = 400_000

val caching = CacheControl.AGGRESSIVE_WEB_CACHE.details

enum class AuthStatus {
    AUTHENTICATED,
    UNAUTHENTICATED
}

/**
 * Examine the request and headers, take proper action, returning a
 * proper response with headers
 */
fun handleRequestAndRespond(sd : ServerData): PreparedResponseData {
    val verb = sd.rd.verb
    val path = sd.rd.path
    val au = sd.au
    val tru = sd.tru
    val rd = sd.rd

    /**
     * The user currently logged in
     */
    val user = rd.user

    /**
     * The data sent in the POST body
     */
    val data = rd.data

    if (verb == Verb.INVALID) {
        return handleBadRequest()
    }

    val authStatus : AuthStatus = isAuthenticated(user)

    return when (Pair(verb, path)){
        Pair(Verb.GET, ""),
        Pair(Verb.GET, HOMEPAGE.path)  -> doGETAuthAndUnauth(authStatus, { generateAuthHomepage(user.name)}, { generateUnAuthenticatedHomepage()})
        Pair(Verb.GET, ENTER_TIME.path) -> doGETRequireAuth(authStatus) { EnterTimeAPI.generateEnterTimePage(tru, user.name) }
        Pair(Verb.GET, TIMEENTRIES.path) -> doGETRequireAuth(authStatus) { EnterTimeAPI.generateTimeEntriesPage(tru, user) }
        Pair(Verb.GET, CREATE_EMPLOYEE.path) -> doGETRequireAuth(authStatus) { EmployeeAPI.generateCreateEmployeePage(user.name) }
        Pair(Verb.GET, EMPLOYEES.path) -> doGETRequireAuth(authStatus) { EmployeeAPI.generateExistingEmployeesPage(user.name, tru) }
        Pair(Verb.GET, LOGIN.path) -> doGETRequireUnauthenticated(authStatus) { LoginAPI.generateLoginPage() }
        Pair(Verb.GET, REGISTER.path) -> doGETRequireUnauthenticated(authStatus) { RegisterAPI.generateRegisterUserPage(tru) }
        Pair(Verb.GET, CREATE_PROJECT.path) -> doGETRequireAuth(authStatus) { ProjectAPI.generateCreateProjectPage(user.name) }
        Pair(Verb.GET, LOGOUT.path) -> doGETRequireAuth(authStatus) { generateLogoutPage(au, user) }
        Pair(Verb.GET, LOGGING.path) -> doGETRequireAuth(authStatus) { LoggingAPI.generateLoggingConfigPage() }

        // posts
        Pair(Verb.POST, ENTER_TIME.path) -> doPOSTAuthenticated(authStatus, EnterTimeAPI.requiredInputs, data) { EnterTimeAPI.handlePOST(tru, user.employeeId, data) }
        Pair(Verb.POST, CREATE_EMPLOYEE.path) -> doPOSTAuthenticated(authStatus, EmployeeAPI.requiredInputs, data) { EmployeeAPI.handlePOST(tru, data) }
        Pair(Verb.POST, LOGIN.path) -> doPOSTRequireUnauthenticated(authStatus, LoginAPI.requiredInputs, data) { LoginAPI.handlePOST(au, data) }
        Pair(Verb.POST, REGISTER.path) -> doPOSTRequireUnauthenticated(authStatus, RegisterAPI.requiredInputs, data) { RegisterAPI.handlePOST(au, data) }
        Pair(Verb.POST, CREATE_PROJECT.path) -> doPOSTAuthenticated(authStatus, ProjectAPI.requiredInputs, data) { ProjectAPI.handlePOST(tru, data) }
        Pair(Verb.POST, LOGGING.path) -> doPOSTAuthenticated(authStatus, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }

        else -> {
            handleStaticFiles(rd.path)
        }
    }
}

/**
 * If we are authenticated, runs some calculations.  Otherwise
 * redirects to the homepage.
 * @param generator the code to run to generate a string to return for this GET
 */
fun doGETRequireAuth(authStatus : AuthStatus, generator : () -> String): PreparedResponseData {
    return when (authStatus) {
        AuthStatus.AUTHENTICATED -> okHTML(generator())
        AuthStatus.UNAUTHENTICATED -> redirectTo(HOMEPAGE.path)
    }
}

/**
 * This is the method for when we want to go either one direction
 * if authenticated or another if unauthenticated.  Most likely
 * example: the homepage
 */
fun doGETAuthAndUnauth(authStatus : AuthStatus, generatorAuthenticated : () -> String, generatorUnauth : () -> String): PreparedResponseData {
    return when (authStatus) {
        AuthStatus.AUTHENTICATED -> okHTML(generatorAuthenticated())
        AuthStatus.UNAUTHENTICATED -> okHTML(generatorUnauth())
    }
}

/**
 * This is for those odd cases where you aren't allowed to go
 * there if you *are* authenticated, like the login page or
 * register user page
 */
fun doGETRequireUnauthenticated(authStatus : AuthStatus, generator : () -> String): PreparedResponseData {
    return when (authStatus) {
        AuthStatus.UNAUTHENTICATED -> okHTML(generator())
        AuthStatus.AUTHENTICATED -> redirectTo(HOMEPAGE.path)
    }
}

/**
 * Handle the (pretty ordinary) situation where a user POSTS data to us
 * and they have to be authenticated to do so
 * @param handler the method run to handle the POST
 */
fun doPOSTAuthenticated(authStatus : AuthStatus,
                        requiredInputs : Set<String>,
                        data : Map<String, String>,
                        handler: () -> PreparedResponseData) : PreparedResponseData {
    return when (authStatus) {
        AuthStatus.AUTHENTICATED -> {
            checkHasExactInputs(data.keys, requiredInputs)
            handler()
        }
        AuthStatus.UNAUTHENTICATED -> handleUnauthorized()
    }
}

/**
 * Handle the (rare) situation where a user POSTS data to us
 * and they *must NOT be* authenticated.  Like on the register user page.
 * @param handler the method run to handle the POST
 */
fun doPOSTRequireUnauthenticated(authStatus : AuthStatus,
                        requiredInputs : Set<String>,
                        data : Map<String, String>,
                        handler: () -> PreparedResponseData) : PreparedResponseData {
    return when (authStatus) {
        AuthStatus.UNAUTHENTICATED -> {
            checkHasExactInputs(data.keys, requiredInputs)
            handler()
        }
        AuthStatus.AUTHENTICATED -> redirectTo(AUTHHOMEPAGE.path)
    }
}

/**
 * A simple cache for the static files.  See [handleStaticFiles]
 */
object HttpResponseCache {
    val staticFileCache = mutableMapOf<String, PreparedResponseData>()

    /**
     * Clears the cache.  This is just used for testing.
     */
    fun clearCache() {
        staticFileCache.clear()
    }
}

/**
 * If the user asks for a path we don't know about, try reading
 * that file from our resources directory
 */
fun handleStaticFiles(path: String): PreparedResponseData {
    // check the cache.  If we have what they want already, just return it.
    // since the files of this application are stored in its resources
    // file and won't be expected to change while the program is running,
    // may as well cache what we can.
    val cachedStaticValue = staticFileCache[path]
    if (cachedStaticValue != null) {
        return cachedStaticValue
    }

    return readStaticFile(path)
}

@Synchronized
private fun readStaticFile(path: String): PreparedResponseData {
    // it is true that this is duplicated code, however
    // the difference is that we are now inside a synchronized
    // block, meaning from here until the end of this scope,
    // there can only be one thread at a time running the
    // code.  The reason it is necessary to duplicate this here
    // is because if two threads hit the previous code that
    // looks like this, they could both end up deciding there
    // is nothing in the cache.  One of them would then get
    // into this code block, the other would be waiting.
    //
    // when the next thread tries coming in, it will immediately
    // his this code, and if the preceeding thread cached what
    // the subsequent thread needed, it can use that.
    val cachedStaticValue = staticFileCache[path]
    if (cachedStaticValue != null) {
        return cachedStaticValue
    }

    val fileContents = FileReader.read(path)
    val result = if (fileContents == null) {
        logDebug("unable to read a file named $path")
        handleNotFound()
    } else {
        when {
            path.takeLast(4) == ".css" -> okCSS(fileContents)
            path.takeLast(3) == ".js" -> okJS(fileContents)
            path.takeLast(4) == ".jpg" -> okJPG(fileContents)
            path.takeLast(5) == ".webp" -> okWEBP(fileContents)
            path.takeLast(5) == ".html" ||
            path.takeLast(4) == ".htm"   -> okHTML(fileContents)
            else -> handleNotFound()
        }
    }

    staticFileCache[path] = result
    return result
}

fun isAuthenticated(u : User) : AuthStatus {
    return if (u != NO_USER) {
        AuthStatus.AUTHENTICATED
    } else {
        AuthStatus.UNAUTHENTICATED
    }
}

/**
 * If you are responding with a success message and it is HTML
 */
fun okHTML(contents : String) =
        ok(toBytes(contents), listOf(ContentType.TEXT_HTML.value))

/**
 * If you are responding with a success message and it is HTML
 */
fun okHTML(contents : ByteArray) =
        ok(contents, listOf(ContentType.TEXT_HTML.value))

/**
 * If you are responding with a success message and it is CSS
 */
fun okCSS(contents : ByteArray) =
        ok(contents, listOf(ContentType.TEXT_CSS.value, caching))
/**
 * If you are responding with a success message and it is JavaScript
 */
fun okJS (contents : ByteArray) =
        ok(contents, listOf(ContentType.APPLICATION_JAVASCRIPT.value, caching))

fun okJPG (contents : ByteArray) =
    ok(contents, listOf(ContentType.IMAGE_JPEG.value, caching))

fun okWEBP (contents : ByteArray) =
        ok(contents, listOf(ContentType.IMAGE_WEBP.value, caching))

private fun ok (contents: ByteArray, ct : List<String>) =
        PreparedResponseData(contents, StatusCode.OK, ct)

/**
 * Use this to redirect to any particular page
 */
fun redirectTo(path: String): PreparedResponseData {
    return PreparedResponseData("", StatusCode.SEE_OTHER, listOf(ContentType.TEXT_HTML.value, "Location: $path"))
}

/**
 * This is our regex for looking at a client's request
 * and determining what to send them.  For example,
 * if they send GET /sample.html HTTP/1.1, we send them sample.html
 *
 * On the other hand if it's not a well formed request, or
 * if we don't have that file, we reply with an error page
 */
val serverStatusLineRegex = """(GET|POST) /(.*) HTTP/(?:1.1|1.0)""".toRegex()

/**
 * This is the regex used to analyze a status line sent by the server and
 * read by the client.  Servers will send messages like: "HTTP/1.1 200 OK" or "HTTP/1.1 500 Internal Server Error"
 * See [StatusCode]
 */
private val clientStatusLineRegex = """HTTP/(?:1.1|1.0) (\d{3}) .*$""".toRegex()

/**
 * Used for extracting the length of the body, in POSTs and
 * responses from servers
 */
val contentLengthRegex = """[cC]ontent-[lL]ength: (.*)$""".toRegex()

/**
 * Used to extract cookies from the Cookie header
 */
private val cookieRegex = """[cC]ookie: (.*)$""".toRegex()

/**
 * Within a cookie header, we want our sessionId cookie, which
 * this regular expression will find
 */
private val sessionIdCookieRegex = """sessionId=(.*)""".toRegex()

/**
 * Analyze the data following HTTP protocol and create a
 * [AnalyzedHttpData] to store the vital information
 */
fun parseHttpMessage(socketWrapper: ISocketWrapper, au: IAuthenticationUtilities): AnalyzedHttpData {
    // read the first line for the fundamental request
    val statusLine = socketWrapper.readLine()
    logTrace("statusLine: $statusLine")
    if (statusLine.isNullOrBlank()) {
        return AnalyzedHttpData(Verb.CLIENT_CLOSED_CONNECTION, rawData = null)
    }

    return when {
        clientStatusLineRegex.containsMatchIn(statusLine) -> analyzeAsClient(checkNotNull(clientStatusLineRegex.matchEntire(statusLine)), socketWrapper)
        serverStatusLineRegex.containsMatchIn(statusLine) -> analyzeAsServer(checkNotNull(serverStatusLineRegex.matchEntire(statusLine)), socketWrapper, au)
        else -> AnalyzedHttpData(Verb.INVALID)
    }

}

/**
 * If we are reviewing an HTTP message as a server
 */
private fun analyzeAsServer(statusLineMatches: MatchResult, socketWrapper: ISocketWrapper, au: IAuthenticationUtilities): AnalyzedHttpData {
    val (verb, path) = parseStatusLineAsServer(statusLineMatches)
    val headers = getHeaders(socketWrapper)

    val token = extractSessionTokenFromHeaders(headers) ?: ""
    val user = extractUserFromAuthToken(token, au)
    val (data, rawData) = extractData(socketWrapper, headers)

    return AnalyzedHttpData(verb, path, data, user, token, headers, rawData)
}

/**
 * If we are reviewing an HTTP message as a client
 */
private fun analyzeAsClient(statusLineMatches: MatchResult, socketWrapper: ISocketWrapper): AnalyzedHttpData {
    val statusCode = parseStatusLineAsClient(statusLineMatches)
    val headers = getHeaders(socketWrapper)
    val rawData = if (headers.any { it.toLowerCase().startsWith(CONTENT_LENGTH)}) {
        val length = extractLengthOfPostBodyFromHeaders(headers)
        socketWrapper.read(length)
    } else {
        null
    }

    return AnalyzedHttpData(statusCode = statusCode, headers = headers, rawData = rawData)
}

/**
 * read the body if one exists and convert it to a string -> string map
 */
private fun extractData(server: ISocketWrapper, headers: List<String>) : Pair<Map<String, String>, String?> {
    return if (headers.any { it.toLowerCase().startsWith(CONTENT_LENGTH)}) {
        val length = extractLengthOfPostBodyFromHeaders(headers)
        val body = server.read(length)
        Pair(parsePostedData(body), body)
    } else {
        Pair(emptyMap(), null)
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
 * The first line tells us a lot. See [serverStatusLineRegex]
 */
fun parseStatusLineAsServer(matchResult: MatchResult): Pair<Verb, String> {
    val verb: Verb = Verb.valueOf(checkNotNull(matchResult.groups[1]){"The HTTP verb must not be missing"}.value)
    logTrace("verb from client was: $verb")
    val file = checkNotNull(matchResult.groups[2]){"The requested path must not be missing"}.value
    logTrace("path from client was: $file")
    return Pair(verb, file)
}

/**
 * The first line tells us a lot. See [clientStatusLineRegex]
 * Also see https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
 */
fun parseStatusLineAsClient(matchResult: MatchResult): StatusCode {
    val statusCode: StatusCode = StatusCode.fromCode(checkNotNull(matchResult.groups[1]){"The status code must not be missing"}.value)
    logTrace("status code from client was: $statusCode")
    return statusCode
}

/**
 * Given the list of headers, find the one with the length of the
 * body of the POST and return that value as a simple [Int]
 */
fun extractLengthOfPostBodyFromHeaders(headers: List<String>): Int {
    require(headers.isNotEmpty()) {"We must receive at least one header at this point or the request is invalid"}
    try {
        val lengthHeader: String = headers.single { it.toLowerCase().startsWith(CONTENT_LENGTH) }
        val length: Int? = contentLengthRegex.matchEntire(lengthHeader)?.groups?.get(1)?.value?.toInt()
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
 * See here for the encoding: https://developer.mozilla.org/en-US/docs/Glossary/percent-encoding
 *
 * for example, valuea=3&valueb=this+is+something
 */
fun parsePostedData(input: String): Map<String, String> {
    require(input.isNotEmpty()) {"The POST body was empty"}
    val postedPairs = mutableMapOf<String, String>()
    val splitByAmpersand = input.split("&")
    for(s : String in splitByAmpersand) {
        val pair = s.split("=")
        check(pair.size == 2) {"Splitting on = should return 2 values.  Input was $s"}
        val result = postedPairs.put(pair[0], decode(pair[1]))
        if (result != null) {
            throw DuplicateInputsException("${pair[0]} was duplicated in the post body - had values of $result and ${pair[1]}")
        }
    }
    return postedPairs
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
        if (!header.isNullOrBlank()) {
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
    logTrace("Assembling data just before shipping to client")
    val status = "HTTP/1.1 ${data.statusCode.value}"
    logTrace("status: $status")
    val contentLengthHeader = "Content-Length: ${data.fileContents.size}"

    val otherHeaders = data.headers.joinToString(CRLF)
    logTrace("contentLengthHeader: $contentLengthHeader")
    data.headers.forEach{logTrace("sending header: $it")}
    server.write("$status$CRLF" +
            "$contentLengthHeader$CRLF" +
            otherHeaders +
            CRLF +
            CRLF)
    server.writeBytes(data.fileContents)
}

