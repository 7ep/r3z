package coverosR3z.exceptions

/**
 * If the database is irretrievably corrupted
 */
class DatabaseCorruptedException(message: String) : Exception(message)