package coverosR3z.server

enum class Verb {
    GET,
    POST,

    /**
     * Used for when we get a malformed request
     */
    INVALID
}