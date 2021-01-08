package coverosR3z.server.types

/**
 * An interface for when an endpoint replies to a GET
 */
interface GetEndpoint : Api {

    /**
     * Note you will need to consider authentication properly
     * here.  See the authentication mechanisms at [coverosR3z.server.utility.AuthUtilities]
     */
    fun handleGet(sd : ServerData) : PreparedResponseData
}