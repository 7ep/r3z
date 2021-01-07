package coverosR3z.misc.exceptions

/**
 * Something failed while trying to parse the options
 * the user provided us at startup.
 */
class ServerOptionsException(message: String) : Exception(message)