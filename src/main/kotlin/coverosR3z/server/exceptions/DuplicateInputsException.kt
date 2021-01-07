package coverosR3z.server.exceptions

/**
 * This is thrown when we try to parse data from the client (sent
 * by the browser) and we get duplicates of a key, like foo=abc&foo=123
 */
class DuplicateInputsException(message: String) : Exception(message)