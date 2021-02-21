package coverosR3z.server.types

/**
 * When we examine the request sent to us by a client,
 * it will look like this:
 *    GET /whatever?foo=bar HTTP/1.1
 *
 * This class serves as a container for those elements, particularly
 * the verb (in this case, GET), the path (above, "whatever"), the
 * query string (above, foo=bar)
 */
data class StatusLine(
    val verb: Verb,
    val path: String,
    val queryString: Map<String, String> = mapOf(),
    val rawQueryString: String = ""
    )
