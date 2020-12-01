package coverosR3z.server

enum class ResponseStatus(val value: String) {
    OK("200 OK"),
    NOT_FOUND("404 NOT FOUND"),
    BAD_REQUEST("400 BAD REQUEST"),
    UNAUTHORIZED("401 UNAUTHORIZED"),
    SEE_OTHER("303 SEE OTHER"),
    INTERNAL_SERVER_ERROR("500 INTERNAL SERVER ERROR"),
}