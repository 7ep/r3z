package coverosR3z.server

/**
 * An enumeration of the possible well-known paths requestable by clients
 *
 * @param value is the string that we see as the path in the URL, e.g. GET /entertime
 * @param assocFile is the file we read to send to the user
 */
enum class NamedPaths(val value: String, val assocFile : String = "") {
    ENTER_TIME("entertime", "enter_time.utl"),
    CREATE_EMPLOYEE("createemployee", "create_employee.html"),
    LOGIN("login", "login.html"),
    REGISTER("register", "register.utl"),
    CREATE_PROJECT("createproject", "create_project.html"),
    HOMEPAGE("homepage", "homepage.html"),
    AUTHHOMEPAGE("authhomepage", "homepage.utl"),
    BAD_REQUEST("badrequest", "400error.html"),
    NOT_FOUND("notfound", "404error.html"),
    UNAUTHORIZED("unauthorized", "401error.html"),
}