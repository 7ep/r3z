package coverosR3z.server

/**
 * Data for shipping to the client
 */
data class PreparedResponseData(val fileContents: String, val responseStatus: ResponseStatus, val headers : List<String> = emptyList())