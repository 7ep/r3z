package coverosR3z.server.types

interface PostEndpoint {

    /**
     * Note you will need to consider authentication properly
     * here.  for example:
     * [coverosR3z.server.utility.doGETAuthAndUnauth]
     */
    fun handlePost(sd : ServerData) : PreparedResponseData
}