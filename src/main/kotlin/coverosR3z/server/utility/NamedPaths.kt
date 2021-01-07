package coverosR3z.server.utility

/**
 * An enumeration of the possible well-known paths requestable by clients
 *
 * @param path is the string that we see as the path in the URL, e.g. GET /entertime
 */
enum class NamedPaths(val path: String) {
    // enter time
    ENTER_TIME("entertime"),

    // existing time entries
    TIMEENTRIES("timeentries"),

    // create employee
    CREATE_EMPLOYEE("createemployee"),

    // existing employees
    EMPLOYEES("employees"),

    // login
    LOGIN("login"),

    // logout
    LOGOUT("logout"),

    // register
    REGISTER("register"),

    // create project
    CREATE_PROJECT("createproject"),

    // unauthenticated homepage
    HOMEPAGE("homepage"),

    // authenticated homepage
    AUTHHOMEPAGE("homepage"),

    // Logging configuration - turn on / off logging
    LOGGING("logging"),

}