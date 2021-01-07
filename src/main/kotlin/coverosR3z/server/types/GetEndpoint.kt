package coverosR3z.server.types

/**
 * An interface for when an endpoint replies to a GET
 */
interface GetEndpoint {

    /**
     * Note you will need to consider authentication properly
     * here.  for example:
     * [coverosR3z.server.utility.doGETAuthAndUnauth]
     */
    fun handleGet(sd : ServerData) : PreparedResponseData
}