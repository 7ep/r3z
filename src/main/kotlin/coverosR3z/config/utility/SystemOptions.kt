package coverosR3z.config.utility

import coverosR3z.misc.exceptions.SystemOptionsException
import coverosR3z.misc.utility.checkParseToInt
import kotlin.system.exitProcess

data class SystemOptions(
        /**
         * The port the server will use
         */
        val port : Int = defaultPort,

        /**
         * The secure port
         */
        val sslPort : Int = defaultSSLPort,


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
        val allLoggingOff : Boolean = false,

        /**
         * If set to tru, starts server with
         * all logging on
         */
        val allLoggingOn : Boolean = false,

        /**
         * With this toggled on on, the server won't automatically
         * redirect insecure requests to the ssl endpoint
         */
        val allowInsecure : Boolean = false,

        /**
         * The value set here will be used in such cases
         * as when the application needs to create a URL
         * that links back, properly.  For example,
         *
         * If we set the host to renomad.com, and we set
         * the port to 443, then when we
         * provide a link for registering a new user,
         * it will look a bit like this:
         *
         * https://renomad.com/register?code=abc123
         *
         * This defaults to "localhost"
         */
        val host : String = "localhost",
){


        /**
         * Sets the network port for the application
         * @param port The preferred port, as a string
         */
        fun setPort(port: String) : SystemOptions {
                val makePort = if (port.isNotBlank()) checkParseToInt(port) else defaultPort
                if (makePort < 1 || makePort > 65535) {throw SystemOptionsException("port number was out of range.  Range is 1-65535.  Your input was: $makePort")
                }
                return this.copy(port = makePort)
        }

        private fun setSslPort(port: String): SystemOptions {
                val makePort = if (port.isNotBlank()) {
                        val parsedPort = checkParseToInt(port)
                        if (parsedPort < 1 || parsedPort > 65535) {
                                throw SystemOptionsException("port number was out of range.  Range is 1-65535.  Your input was: $parsedPort")
                        } else {
                                parsedPort
                        }
                } else {
                        defaultSSLPort
                }

                return this.copy(sslPort = makePort)
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

                return this.copy(dbDirectory = makeDBDir)
        }

        fun setLoggingOff(allLoggingOff: String) : SystemOptions {
                return this.copy(allLoggingOff = allLoggingOff == "true")
        }


        fun setLoggingOn(allLoggingOn: String): SystemOptions {
                return this.copy(allLoggingOn = allLoggingOn == "true")
        }

        fun setAllowInsecureOn(allowInsecure: String): SystemOptions {
                return this.copy(allowInsecure = allowInsecure == "true")
        }

        /**
         * Sets the domain name / host name for the application
         */
        fun setHost(host: String) : SystemOptions {
                val myHost = if (host.isNotBlank()) host.trim() else defaultHost
                return this.copy(host = myHost)
        }



        companion object{

                const val defaultHost = "localhost"
                const val defaultPort = 12345
                const val defaultSSLPort = 12443

                /**
                 * Given the command-line arguments, returns the first value
                 * as an integer for use as a port number, or defaults to 12345
                 */
                fun extractOptions(args: Array<String>) : SystemOptions {

                        try {
                                return processArgs(args)
                        }
                        catch (ex: Throwable) {
                                throw SystemOptionsException(ex.message + "\n" + fullHelpMessage)
                        }
                }

                /**
                 * The user can provide command-line options when running this.
                 * See [SystemOptions] and [extractOptions]
                 */
                fun extractCommandLineOptions(args: Array<String>): SystemOptions {
                        return try {
                                extractOptions(args)
                        } catch (ex: SystemOptionsException) {
                                println(ex.message)
                                exitProcess(0)
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
                        val sslPortOption = Option(false, "")
                        val diskPersistenceOption = Option(false, "", isFlag = true)
                        val loggingOffOption = Option(false, "", isFlag = true)
                        val loggingOnOption = Option(false, "", isFlag = true)
                        val allowInsecureOption = Option(false, "", isFlag = true)
                        val hostOption = Option(false, "")

                        val possibleOptions = listOf(
                                OptionGroup("-d", directoryOption),
                                OptionGroup("--dbdirectory", directoryOption),
                                OptionGroup("-p", portOption),
                                OptionGroup("--port", portOption),
                                OptionGroup("-s", sslPortOption),
                                OptionGroup("--sslport", sslPortOption),
                                OptionGroup("--no-disk-persistence", diskPersistenceOption),
                                OptionGroup("--no-logging", loggingOffOption),
                                OptionGroup("--full-logging", loggingOnOption),
                                OptionGroup("--allow-insecure", allowInsecureOption),
                                OptionGroup("-h", hostOption),
                                OptionGroup("--host", hostOption),

                        )

                        val fullInput = args.joinToString(" ")
                        return if (args.isEmpty() || args[0].isBlank()) {
                                SystemOptions()
                        } else {
                                var currentIndex = 0

                                loop@ while (currentIndex < args.size) {
                                        if (args[currentIndex] == "-?") throw SystemOptionsException("")

                                        for (option in possibleOptions) {
                                                if (args[currentIndex].startsWith(option.textValue)) {
                                                        if (option.o.setByUser) throw SystemOptionsException("Duplicate options were provided. This is not allowed, go to jail. your input: $fullInput")
                                                        val (value, increment) = if (option.o.isFlag) Pair("true", 1) else getValueForKey(option.textValue, args, currentIndex, fullInput)
                                                        currentIndex += increment
                                                        option.o.setByUser = true
                                                        option.o.value = value
                                                        continue@loop
                                                }
                                        }
                                        throw SystemOptionsException("argument not recognized: ${args[currentIndex]}")
                                }

                                return SystemOptions()
                                        .setPort(portOption.value)
                                        .setSslPort(sslPortOption.value)
                                        .setDirectory(directoryOption.value, diskPersistenceOption.value)
                                        .setLoggingOff(loggingOffOption.value)
                                        .setLoggingOn(loggingOnOption.value)
                                        .setAllowInsecureOn(allowInsecureOption.value)
                                        .setHost(hostOption.value)
                        }
                }

                /**
                 * Gets the value for a flag.  Returns a pair with the value as the first entry and the increment for the current index as the second entry
                 */
                private fun getValueForKey(keyString : String, args: Array<String>, currentIndex: Int, fullInput: String) : Pair<String, Int> {
                        val concatValue = args[currentIndex].substring(keyString.length)
                        return if (concatValue.isNotEmpty()) {
                                check(concatValue.isNotBlank())
                                if (concatValue.startsWith("-")) throw SystemOptionsException("The $keyString option was provided without a value: $fullInput")
                                Pair(concatValue, 1)
                        } else {
                                val value = try {args[currentIndex + 1]} catch (ex : IndexOutOfBoundsException) {throw SystemOptionsException("The $keyString option was provided without a value: $fullInput")
                                }
                                if (value.startsWith("-")) throw SystemOptionsException("The $keyString option was provided without a value: $fullInput")
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

general help
------------

-?                     prints this help message

Server Ports
------------

--port PORT_NUMBER
-p PORT_NUMBER         set the port number for the server (default 12345)

--sslport PORT_NUMBER
-s PORT_NUMBER         set the ssl port number for the server (default 12443)

--allow-insecure       typically, insecure requests are redirected to https.
                       with this flag, that doesn't automatically happen.
                       

Host name
---------

-h HOSTNAME             
--host HOSTNAME         sets the hostname to this application

Database
--------

--dbdirectory DIRECTORY
-d DIRECTORY           the directory to store data (default /db)

--no-disk-persistence  do not write data to the disk.  Note
                       that this is primarily (exclusively?) for testing

Logging
-------
                       
--no-logging           start the server with all logging turned
                       off, except for "IMPERATIVE"
--full-logging         start with all logging on (default: trace is off, all others on)  
     """.trimIndent()

        }
}