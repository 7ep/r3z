package coverosR3z.server

import coverosR3z.authentication.*
import coverosR3z.domainobjects.NO_USER
import coverosR3z.domainobjects.User
import coverosR3z.logging.logDebug
import coverosR3z.misc.*
import coverosR3z.server.NamedPaths.*
import coverosR3z.timerecording.*
import java.net.URLDecoder

data class ServerData(val au: IAuthenticationUtilities, val tru: ITimeRecordingUtilities, val rd: RequestData)

/**
 *   HTTP/1.1 defines the sequence CR LF as the end-of-line marker for all
 *  protocol elements except the entity-body (see appendix 19.3 for
 *  tolerant applications). The end-of-line marker within an entity-body
 *  is defined by its associated media type, as described in section 3.7.
 *
 *  See https://tools.ietf.org/html/rfc2616
 */
const val CRLF = "\r\n"

val caching = CacheControl.AGGRESSIVE_MINUTE_CACHE.details

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
    val user = rd.user
    val data = rd.data

    if (verb == Verb.INVALID) {
        return handleBadRequest()
    }

    return when (Pair(verb, path)){
        Pair(Verb.GET, ""),
        Pair(Verb.GET, HOMEPAGE.path)  -> doGetHomePage(rd)
        Pair(Verb.GET, ENTER_TIME.path) -> doGETEnterTimePage(tru, rd)
        Pair(Verb.GET, ENTER_TIMEJS.path) -> okJS(enterTimeJS)
        Pair(Verb.GET, ENTER_TIMECSS.path) -> okCSS(enterTimeCSS)
        Pair(Verb.GET, TIMEENTRIES.path) -> doGetTimeEntriesPage(tru, rd)
        Pair(Verb.GET, CREATE_EMPLOYEE.path) -> doGETCreateEmployeePage(rd)
        Pair(Verb.GET, EMPLOYEES.path) -> okHTML(existingEmployeesHTML(rd.user.name.value, tru.listAllEmployees()))
        Pair(Verb.GET, LOGIN.path) -> doGETLoginPage(rd)
        Pair(Verb.GET, REGISTER.path) -> doGETRegisterPage(tru, rd)
        Pair(Verb.GET, CREATE_PROJECT.path) -> doGETCreateProjectPage(rd)
        Pair(Verb.GET, LOGOUT.path) -> doGETLogout(au, rd)
        Pair(Verb.GET, SHUTDOWN_SERVER.path) -> handleGETShutdownServer(rd.user)
        Pair(Verb.GET, LOGGING.path) -> handleGETLogging(user)

        // posts
        Pair(Verb.POST, ENTER_TIME.path) -> handlePOSTTimeEntry(tru, user, data)
        Pair(Verb.POST, CREATE_EMPLOYEE.path) -> handlePOSTNewEmployee(tru, user, data)
        Pair(Verb.POST, LOGIN.path) -> handlePOSTLogin(au, user, data)
        Pair(Verb.POST, REGISTER.path) -> handlePOSTRegister(au, user, data)
        Pair(Verb.POST, CREATE_PROJECT.path) -> handlePOSTCreatingProject(tru, user, data)
        Pair(Verb.POST, LOGGING.path) -> handlePOSTLogging(user, data)

        else -> {
            handleUnknownFiles(rd)
        }
    }
}

/**
 * If the user asks for a path we don't know about, try reading
 * that file from our resources directory
 */
private fun handleUnknownFiles(rd: RequestData): PreparedResponseData {
    val fileContents = FileReader.read(rd.path)

    return if (fileContents == null) {
        logDebug("unable to read a file named ${rd.path}")
        handleNotFound()
    } else {
        when {
            rd.path.takeLast(4) == ".css" -> ok(
                fileContents,
                listOf(ContentType.TEXT_CSS.value, caching)
            )
            rd.path.takeLast(3) == ".js" -> ok(
                fileContents,
                listOf(ContentType.APPLICATION_JAVASCRIPT.value, caching)
            )
            rd.path.takeLast(4) == ".jpg" -> okJPG(fileContents)
            rd.path.takeLast(5) == ".webp" -> okWEBP(fileContents)
            rd.path.takeLast(5) == ".html" -> ok(
                    fileContents,
                    listOf(ContentType.TEXT_HTML.value, caching))
            else -> handleNotFound()
        }
    }
}

fun handleGETShutdownServer(user: User): PreparedResponseData {
    return if (isAuthenticated(user)) {
        SocketCommunication.shouldContinue = false
        okHTML(successHTML)
    } else {
        redirectTo(HOMEPAGE.path)
    }
}


fun isAuthenticated(u : User) : Boolean {
    return u != NO_USER
}

fun isAuthenticated(rd : RequestData) : Boolean {
    return rd.user != NO_USER
}

/**
 * If you are responding with a success message and it is HTML
 */
fun okHTML(contents : String) =
        ok(toBytes(contents), listOf(ContentType.TEXT_HTML.value))

/**
 * If you are responding with a success message and it is CSS
 */
fun okCSS(contents : String) =
        ok(toBytes(contents), listOf(ContentType.TEXT_CSS.value, caching))
/**
 * If you are responding with a success message and it is JavaScript
 */
fun okJS (contents : String) =
        ok(toBytes(contents), listOf(ContentType.APPLICATION_JAVASCRIPT.value, caching))

fun okJPG (contents : ByteArray) =
    ok(contents, listOf(ContentType.IMAGE_JPEG.value, caching))

fun okWEBP (contents : ByteArray) =
        ok(contents, listOf(ContentType.IMAGE_WEBP.value, caching))

private fun ok (contents: ByteArray, ct : List<String>) =
        PreparedResponseData(contents, ResponseStatus.OK, ct)

/**
 * Use this to redirect to any particular page
 */
fun redirectTo(path: String): PreparedResponseData {
    return PreparedResponseData("", ResponseStatus.SEE_OTHER, listOf(ContentType.TEXT_HTML.value, "Location: $path"))
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
        // arbitrarily setting to 10,000 for now
        val maxContentLength = 10_000
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
    require(input.isNotEmpty()) {"The input to parse was empty"}
    // Need to split up '&' separated fields into keys and values and pack into a kotlin map
    // Closures for efficiency ahoy, sorry
    try {
        return (input.split("&").associate { field ->
            field.split("=")
                .let { it[0] to URLDecoder.decode(it[1], Charsets.UTF_8)
                }
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
    val contentLengthHeader = "Content-Length: ${data.fileContents.size}"

    val otherHeaders = data.headers.joinToString(CRLF)
    logDebug("contentLengthHeader: $contentLengthHeader")
    data.headers.forEach{logDebug("sending header: $it")}
    server.write("$status$CRLF" +
            "$contentLengthHeader$CRLF" +
            otherHeaders +
            CRLF +
            CRLF)
    server.writeBytes(data.fileContents)
}

