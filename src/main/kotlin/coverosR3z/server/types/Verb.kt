package coverosR3z.server.types

enum class Verb {
    GET,
    POST,

    /**
     * Used for when we get a malformed request
     */
    INVALID,

    /**
     * Used as a null value, especially valuable
     * when our analysis of the HTTP message shows
     * it is coming from the server (no verb is sent)
     */
    NONE,

    /**
     * If the client closed the connection
     */
    CLIENT_CLOSED_CONNECTION,
}