package coverosR3z

/**
 * The BDD framework requires a source BDD HTML file to exist
 * so we can mark the steps as done.  See, for example,
 * enteringTimeBDD.html.
 *
 * If we cannot find the file, this gets thrown
 */
class BDDFileMissingException(msg : String = "Required BDD file missing") : Throwable(msg)
