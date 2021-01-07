package coverosR3z.config.utility

import coverosR3z.misc.exceptions.ServerOptionsException
import coverosR3z.misc.utility.checkParseToInt

data class SystemOptions(
        /**
         * The port the server will use
         */
        val port : Int = defaultPort,
        /**
         * the directory in which we will store the data
         * if this is set to null, we won't store anything to disk,
         * which is really just useful for testing
         */
        val dbDirectory : String? = "db/",

        /**
         * If this is set to true, start the server with
         * all logging turned off, except IMPERATIVE, which
         * cannot be turned off.
         */
        val allLoggingOff : Boolean = false){


        /**
         * Sets the network port for the application
         * @param port The preferred port, as a string
         */
        fun setPort(port: String) : SystemOptions {
                val makePort = if (port.isNotBlank()) checkParseToInt(port) else defaultPort
                if (makePort < 1 || makePort > 65535) {throw ServerOptionsException("port number was out of range.  Range is 1-65535.  Your input was: $makePort")
                }
                return SystemOptions(port = makePort, dbDirectory = dbDirectory)
        }

        /**
         * Sets the directory where we will store the database files.
         * @param dbDirectory the directory we want
         * @param ndp a flag, no-disk-persistence, means to not every write the database to disk (used for testing)
         */
        fun setDirectory(dbDirectory: String, ndp: String) : SystemOptions {
                check(!(dbDirectory.isNotBlank() && ndp=="true")){"If you're setting the noDiskPersistence option and also a database directory, you're very foolish."}
                val makeDBDir : String? =
                        when {
                                ndp == "true" -> null
                                dbDirectory.isNotBlank() -> if (! dbDirectory.endsWith("/")) "$dbDirectory/" else dbDirectory
                                else -> "db/"
                        }

                return SystemOptions(port = port, dbDirectory = makeDBDir)
        }

        fun setLoggingOff(allLoggingOff: String) : SystemOptions {
                return SystemOptions(
                        port = port,
                        dbDirectory = dbDirectory,
                        allLoggingOff = allLoggingOff == "true")
        }

        companion object{

                const val defaultPort = 12345

                /**
                 * Given the command-line arguments, returns the first value
                 * as an integer for use as a port number, or defaults to 12345
                 */
                fun extractOptions(args: Array<String>) : SystemOptions {

                        try {
                                return processArgs(args)
                        }
                        catch (ex: Throwable) {
                                throw ServerOptionsException(ex.message + "\n" + fullHelpMessage)
                        }
                }

                private fun processArgs(args: Array<String>): SystemOptions {
                        /**
                         * Holds the data for a particular option provided on the command line.  See [SystemOptions.setDirectory]
                         * and [SystemOptions.setPort] and similar others for accepting these values and performing validation.
                         *
                         * The only validation we do here is disallowing duplicate values, like "-p 10 -p 20,"
                         * and also that the value for an option doesn't start with a dash, like "-d -thisiswrong",
                         * or that a key requiring a value does indeed have one, unlike: "-d "
                         */
                        data class Option(var setByUser : Boolean, var value : String, val isFlag : Boolean = false)

                        /**
                         * An option group is a group of ways to access an option, say maybe you
                         * have a short version ("-p") and a long version("--port").
                         * Just have multiple OptionGroup that refer to the same Option
                         */
                        data class OptionGroup(val textValue : String, val o : Option)

                        val directoryOption = Option(false, "")
                        val portOption = Option(false, "")
                        val diskPersistenceOption = Option(false, "", isFlag = true)
                        val loggingOption = Option(false, "", isFlag = true)

                        val possibleOptions = listOf(
                                OptionGroup("-d", directoryOption),
                                OptionGroup("--dbdirectory", directoryOption),
                                OptionGroup("-p", portOption),
                                OptionGroup("--port", portOption),
                                OptionGroup("--no-disk-persistence", diskPersistenceOption),
                                OptionGroup("--no-logging", loggingOption))

                        val fullInput = args.joinToString(" ")
                        return if (args.isEmpty() || args[0].isBlank()) {
                                SystemOptions()
                        } else {
                                var currentIndex = 0

                                loop@ while (currentIndex < args.size) {
                                        if (args[currentIndex] == "-h" || args[currentIndex] == "-?") throw ServerOptionsException("")

                                        for (option in possibleOptions) {
                                                if (args[currentIndex].startsWith(option.textValue)) {
                                                        if (option.o.setByUser) throw ServerOptionsException("Duplicate options were provided. This is not allowed, go to jail. your input: $fullInput")
                                                        val (value, increment) = if (option.o.isFlag) Pair("true", 1) else getValueForKey(option.textValue, args, currentIndex, fullInput)
                                                        currentIndex += increment
                                                        option.o.setByUser = true
                                                        option.o.value = value
                                                        continue@loop
                                                }
                                        }
                                        throw ServerOptionsException("argument not recognized: ${args[currentIndex]}")
                                }

                                return SystemOptions()
                                        .setPort(portOption.value)
                                        .setDirectory(directoryOption.value, diskPersistenceOption.value)
                                        .setLoggingOff(loggingOption.value)
                        }
                }

                /**
                 * Gets the value for a flag.  Returns a pair with the value as the first entry and the increment for the current index as the second entry
                 */
                private fun getValueForKey(keyString : String, args: Array<String>, currentIndex: Int, fullInput: String) : Pair<String, Int> {
                        val concatValue = args[currentIndex].substring(keyString.length)
                        return if (concatValue.isNotEmpty()) {
                                check(concatValue.isNotBlank())
                                if (concatValue.startsWith("-")) throw ServerOptionsException("The $keyString option was provided without a value: $fullInput")
                                Pair(concatValue, 1)
                        } else {
                                val value = try {args[currentIndex + 1]} catch (ex : IndexOutOfBoundsException) {throw ServerOptionsException("The $keyString option was provided without a value: $fullInput")
                                }
                                if (value.startsWith("-")) throw ServerOptionsException("The $keyString option was provided without a value: $fullInput")
                                Pair(value, 2)
                        }
                }

                val fullHelpMessage = """
Here is some help for running this application.
        
You can provide options when running this, to change its configuration.

Sample: 
    The following runs the application with the
    port set to 54321 and the database directory
    set to "db/" (make sure to end the directory
    with a forward slash):
    
    java -jar r3z.jar -p 54321 -d db/
    
The options available are:

-h                     prints this help message
-p PORT_NUMBER         set the port number for the server
-d DIRECTORY           the directory to store data
--no-disk-persistence  do not write data to the disk.  Note
                       that this is primarily (exclusively?) for testing
--no-logging           start the server with all logging turned
                       off, except for "IMPERATIVE"
    """.trimIndent()

        }
}