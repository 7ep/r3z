package coverosR3z.server

/**
 * Possible behaviors of the server
 */
enum class ActionType {
    /**
     * Just read a file, plain and simple
     */
    READ_FILE,

    /**
     * This file will require rendering
     */
    TEMPLATE,

    /**
     * The server has sent us data with a post.
     * We have to handle it before we respond
     */
    HANDLE_POST_FROM_CLIENT,

    /**
     * The client sent us a bad (malformed) request
     */
    BAD_REQUEST,

    /**
     * Cascading style sheet
     */
    CSS,

    /**
     * A JavaScript file
     */
    JS,
}