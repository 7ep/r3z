package coverosR3z.exceptions

/**
 * InexactInputsException will be thrown whenever we get anything
 * different than *precisely* the values we expect to receive, and
 * we won't do anything else past that.
 *
 * This exception will hold a message indicating which were the exact
 * inputs expected.
 */
class InexactInputsException(message: String) : Exception(message)