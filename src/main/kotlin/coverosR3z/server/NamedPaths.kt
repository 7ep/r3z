package coverosR3z.server

/**
 * An enumeration of the possible well-known paths requestable by clients
 */
enum class NamedPaths(val value: String, val assocFile : String = "") {
    ENTER_TIME("entertime", "enter_time.utl"),
    CREATE_EMPLOYEE("createemployee", "create_employee.html"),
    LOGIN("login", "login.html"),
    REGISTER("register", "register.html"),
    CREATE_PROJECT("createproject", "create_project.html"),
    HOMEPAGE("homepage", "homepage.html"),
    AUTHHOMEPAGE("authhomepage", "homepage.utl"),
}