package coverosR3z.server

/**
 * An enumeration of the possible well-known paths requestable by clients
 *
 * @param path is the string that we see as the path in the URL, e.g. GET /entertime
 */
enum class NamedPaths(val path: String) {
    // enter time
    ENTER_TIME("entertime"),
    ENTER_TIMEJS("entertime.js"),
    ENTER_TIMECSS("entertime.css"),

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
    REGISTERCSS("register.css"),

    // create project
    CREATE_PROJECT("createproject"),

    // unauthenticated homepage
    HOMEPAGE("homepage"),

    // authenticated homepage
    AUTHHOMEPAGE("homepage"),

    /**
     * A special page, this shuts the server down
     */
    SHUTDOWN_SERVER("shutdown"),
}