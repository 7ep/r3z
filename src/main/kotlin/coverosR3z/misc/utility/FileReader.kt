package coverosR3z.misc.utility

import java.net.URL
import java.util.*

fun toBytes(value : String) : ByteArray {
    return value.toByteArray(Charsets.UTF_8)
}

fun toStr(value : ByteArray) : String {
    return value.toString(Charsets.UTF_8)
}

class FileReader {

    companion object {

        /**
         * Read a file
         */
        fun read(filename: String) : ByteArray? {
            val file = this::class.java.classLoader.getResource(filename)
                    ?: return null
            return file.readBytes()
        }

        fun getResources(name: String): Enumeration<URL>? {
            return this::class.java.classLoader.getResources(name)
        }

    }
}