package coverosR3z.templating

import java.lang.IllegalArgumentException

class FileReader {
    companion object {

        /**
         * Just like [read] but throws an exception if it
         * doesn't find the file
         */
        fun readNotNull(filename: String) : String {
            return checkNotNull(read(filename))
        }

        /**
         * Read in template file as a string
         */
        fun read(filename: String) : String? {
            val foo = this::class.java.classLoader.getResource(filename)
                    ?: return null
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