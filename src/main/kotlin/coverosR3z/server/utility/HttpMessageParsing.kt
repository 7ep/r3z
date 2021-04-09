package coverosR3z.server.utility

import coverosR3z.logging.ILogger
import coverosR3z.misc.utility.decode
import coverosR3z.server.exceptions.DuplicateInputsException
import coverosR3z.server.types.*


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
 * See [coverosR3z.server.types.StatusCode]
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
 * The content length is the size of the body of the HTTP request.
 * We want to put some cap on it, for now, since our current expected use
 * case doesn't account for just endlessly huge content.  Maybe when
 * we include video streaming that will change.
 */
const val CONTENT_LENGTH = "Content-Length"
const val maxContentLength = 400_000

/**
 * Putting a cap on the size of query strings,
 * just to keep things within some kind of bounds.
 */
const val maxQueryStringLength = 1024

/**
 * Analyze the data following HTTP protocol and create a
 * [AnalyzedHttpData] to store the vital information
 */
fun parseHttpMessage(socketWrapper: ISocketWrapper, logger: ILogger): AnalyzedHttpData {
    // read the first line for the fundamental request
    val statusLine = socketWrapper.readLine()
    logger.logTrace { "statusLine: $statusLine" }
    if (statusLine.isNullOrBlank()) {
        return AnalyzedHttpData(Verb.CLIENT_CLOSED_CONNECTION)
    }

    return when {
        serverStatusLineRegex.containsMatchIn(statusLine) ->
            analyzeAsServer(checkNotNull(serverStatusLineRegex.matchEntire(statusLine)), socketWrapper, logger)
        else -> AnalyzedHttpData(Verb.INVALID)
    }

}

/**
 * Analyze a message as a client (not as a server)
 * Analyze the data following HTTP protocol and create a
 * [AnalyzedHttpData] to store the vital information
 */
fun parseHttpMessageAsClient(socketWrapper: ISocketWrapper, logger: ILogger): AnalyzedHttpData {
    // read the first line for the fundamental request
    val statusLine = socketWrapper.readLine()
    logger.logTrace { "statusLine: $statusLine" }
    if (statusLine.isNullOrBlank()) {
        return AnalyzedHttpData(Verb.CLIENT_CLOSED_CONNECTION)
    }

    return analyzeAsClient(checkNotNull(clientStatusLineRegex.matchEntire(statusLine)), socketWrapper, logger)
}



/**
 * If we are reviewing an HTTP message as a server
 */
private fun analyzeAsServer(
    statusLineMatches: MatchResult,
    socketWrapper: ISocketWrapper,
    logger: ILogger): AnalyzedHttpData {

    val statusLine = parseStatusLineAsServer(statusLineMatches, logger)
    val headers = getHeaders(socketWrapper)

    val token = extractSessionTokenFromHeaders(headers) ?: ""
    val postBodyData = extractData(socketWrapper, headers)

    return AnalyzedHttpData(
        statusLine.verb,
        statusLine.path,
        statusLine.queryString,
        statusLine.rawQueryString,
        postBodyData,
        sessionToken = token,
        headers = headers)
}

/**
 * If we are reviewing an HTTP message as a client
 */
private fun analyzeAsClient(statusLineMatches: MatchResult, socketWrapper: ISocketWrapper, logger: ILogger): AnalyzedHttpData {
    val statusCode = parseStatusLineAsClient(statusLineMatches, logger)
    val headers = getHeaders(socketWrapper)
    val length = extractLengthOfPostBodyFromHeaders(headers)
    val rawData = socketWrapper.read(length)

    return AnalyzedHttpData(statusCode = statusCode, headers = headers, data = PostBodyData(rawData = rawData))
}

/**
 * read the body if one exists and convert it to a string -> string map
 */
private fun extractData(server: ISocketWrapper, headers: List<String>) : PostBodyData {
    return if (headers.any { it.toLowerCase().startsWith(CONTENT_LENGTH.toLowerCase())}) {
        val length = extractLengthOfPostBodyFromHeaders(headers)
        val body = server.read(length)
        PostBodyData(parseUrlEncodedForm(body), body)
    } else {
        PostBodyData()
    }
}

/**
 * The first line tells us a lot. See [serverStatusLineRegex]
 */
fun parseStatusLineAsServer(matchResult: MatchResult, logger: ILogger): StatusLine {
    val verb: Verb = Verb.valueOf(checkNotNull(matchResult.groups[1]){"The HTTP verb must not be missing"}.value)
    logger.logTrace { "verb from client was: $verb" }
    val pathAndQuery = checkNotNull(matchResult.groups[2]){"The requested path must not be missing"}.value
    logger.logTrace { "full path from client was: $pathAndQuery" }
    val split = pathAndQuery.split("?")
    check(split.size in 1..2)
    val path = split[0]
    val (rawQueryString, queryString) = if (split.size == 2)  {
        val rawQueryString = split[1]
        check(rawQueryString.length < maxQueryStringLength) { "query string exceeded maximum allowed length" }
        Pair(rawQueryString, parseUrlEncodedForm(rawQueryString))
    } else Pair("", mapOf())
    return StatusLine(verb, path, queryString, rawQueryString)
}

/**
 * The first line tells us a lot. See [clientStatusLineRegex]
 * Also see https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
 */
fun parseStatusLineAsClient(matchResult: MatchResult, logger: ILogger): StatusCode {
    val statusCode: StatusCode = StatusCode.fromCode(checkNotNull(matchResult.groups[1]){"The status code must not be missing"}.value)
    logger.logTrace { "status code from client was: $statusCode" }
    return statusCode
}

/**
 * Given the list of headers, find the one with the length of the
 * body of the POST and return that value as a simple [Int]
 */
fun extractLengthOfPostBodyFromHeaders(headers: List<String>): Int {
    require(headers.isNotEmpty()) {"We must receive at least one header at this point or the request is invalid"}
    try {
        val lengthHeader: String = headers.single { it.toLowerCase().startsWith(CONTENT_LENGTH.toLowerCase()) }
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
fun parseUrlEncodedForm(input: String): Map<String, String> {
    if (input.isEmpty()) return emptyMap()
    val postedPairs = mutableMapOf<String, String>()
    val splitByAmpersand = input.split("&")
    for(s : String in splitByAmpersand) {
        val pair = s.split("=")
        check(pair.size == 2) {"Splitting on = should return 2 values.  Input was $s"}
        check(pair[0].isNotBlank()) {"The key must not be blank"}
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
