package coverosR3z.server.types

/**
 * Contains the data sent from a HTTP POST
 * @param mapping a map of key to value, extracted from a url-encoded
 *             body that might look like this: foo=123&bar=abc ,
 *             which would give a map of "foo" to "123" and "bar" to "abc"
 * @param rawData the raw string of the POST body, before processing
 */
data class PostBodyData(val mapping : Map<String, String> = emptyMap(), val rawData: String? = null)