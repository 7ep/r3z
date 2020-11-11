package coverosR3z.server

/**
 * An enumeration of the possible pages requestable by clients
 */
enum class TargetPage(val value: String) {
    ENTER_TIME("entertime"),
    CREATE_EMPLOYEE("createemployee"),
    LOGIN("login"),
    REGISTER("register"),
    CREATE_PROJECT("createproject"),
}