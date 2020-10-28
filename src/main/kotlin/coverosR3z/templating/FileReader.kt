package coverosR3z.templating

import java.lang.IllegalArgumentException

class FileReader {
    companion object {

        /**
         * Read in template file as a string
         */
        fun read(filename: String) : String {
            val foo = this::class.java.classLoader.getResource(filename)
                    ?: throw IllegalArgumentException("Could not read a filename of $filename")
            return foo.readBytes().toString(Charsets.UTF_8)
        }

        /**
         * Returns true if the requested file exists in resources
         */
        fun exists(filename: String) : Boolean {
            return this::class.java.classLoader.getResource(filename) != null
        }
    }
}