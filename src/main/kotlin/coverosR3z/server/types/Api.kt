package coverosR3z.server.types

interface Api {

    /**
     * Set the path to this endpoint
     *
     * For example, if this is for the HomepageAPI, then
     * the text should be "homepage", as in http://HOST/homepage
     */
    val path: String

}