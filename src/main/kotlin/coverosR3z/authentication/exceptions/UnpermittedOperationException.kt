package coverosR3z.authentication.exceptions

/**
 * This exception will be thrown when something is attempted
 * which the [coverosR3z.authentication.types.CurrentUser] has
 * no authorization to do
 */
class UnpermittedOperationException(msg: String) : Exception(msg)
