package coverosR3z.server.types

enum class StatusCode(val value: String, val code: String) {
    OK("200 OK", "200"),
    NOT_FOUND("404 NOT FOUND", "404"),
    BAD_REQUEST("400 BAD REQUEST", "400"),

    /**
     * Unauthorized basically means unauthenticated.  The user
     * isn't logged in
     */
    UNAUTHORIZED("401 UNAUTHORIZED", "401"),

    /**
     * Forbidden means that the user isn't allowed to do this
     * action, but they are rcognized by the system.  They are
     * authenticated, but not authorized, for this action.
     */
    FORBIDDEN("403 FORBIDDEN", "403"),

    /**
     * A redirect to another page.
     */
    SEE_OTHER("303 SEE OTHER", "303"),

    /**
     * General server errors.  A catch-all for any exceptions
     * that are able to bubble to the top.
     */
    INTERNAL_SERVER_ERROR("500 INTERNAL SERVER ERROR", "500"),

    /**
     * None is used when there is no status code, like when we are
     * incorporating this type as part of a general analysis of a
     * HTTP message and it's coming from the client.
     */
    NONE("","");

    companion object {
        fun fromCode(code : String): StatusCode {
            return values().single{it.code == code}
        }
    }
}