package coverosR3z.persistence

/**
 * This is thrown whenever data is changed in such a way
 * as to create data lacking integrity.  For example,
 * adding a time entry with a project id where no such
 * project exists.
 */
class ProjectIntegrityViolationException : Exception()
class EmployeeIntegrityViolationException : Exception()
