package coverosR3z.server.utility

import coverosR3z.config.STATIC_FILES_DIRECTORY
import coverosR3z.logging.logImperative
import coverosR3z.logging.logTrace
import coverosR3z.misc.utility.FileReader
import coverosR3z.misc.utility.toBytes
import coverosR3z.server.api.handleNotFound
import coverosR3z.server.types.*
import java.nio.file.*


/**
 *   HTTP/1.1 defines the sequence CR LF as the end-of-line marker for all
 *  protocol elements except the entity-body (see appendix 19.3 for
 *  tolerant applications). The end-of-line marker within an entity-body
 *  is defined by its associated media type, as described in section 3.7.
 *
 *  See https://tools.ietf.org/html/rfc2616
 */
const val CRLF = "\r\n"

val caching = CacheControl.AGGRESSIVE_WEB_CACHE.details

class ServerUtilities {
    companion object {


        /**
         * This is used at server startup to load the cache with all
         * our static files.
         *
         * The code for looping through the files in the jar was
         * harder than I thought, since we're asking to loop through
         * a zip file, not an ordinary file system.
         *
         * Maybe some opportunity for refactoring here.
         */
        fun loadStaticFilesToCache(cache: MutableMap<String, PreparedResponseData>) {
            logImperative("Loading all static files into cache")

            val urls = checkNotNull(FileReader.getResources(STATIC_FILES_DIRECTORY))
            for (url in urls) {
                val uri = url.toURI()

                val myPath = if (uri.scheme == "jar") {
                    val fileSystem: FileSystem = FileSystems.newFileSystem(uri, emptyMap<String, Any>())
                    fileSystem.getPath(STATIC_FILES_DIRECTORY)
                } else {
                    Paths.get(uri)
                }

                for (path: Path in Files.walk(myPath, 1)) {
                    val fileContents = FileReader.read(STATIC_FILES_DIRECTORY + path.fileName.toString()) ?: continue
                    val filename = path.fileName.toString()
                    val result =
                        when {
                            filename.takeLast(4) == ".css" -> okCSS(fileContents)
                            filename.takeLast(3) == ".js" -> okJS(fileContents)
                            filename.takeLast(4) == ".jpg" -> okJPG(fileContents)
                            filename.takeLast(5) == ".webp" -> okWEBP(fileContents)
                            filename.takeLast(5) == ".html" || filename.takeLast(4) == ".htm" -> okHTML(fileContents)
                            else -> handleNotFound()
                        }

                    cache[filename] = result
                    logTrace { "Added $filename to the cache" }
                }
            }
        }

        /**
         * If you are responding with a success message and it is HTML
         */
        fun okHTML(contents : String) =
                ok(toBytes(contents), listOf(ContentType.TEXT_HTML.value))

        /**
         * If you are responding with a success message and it is HTML
         */
        fun okHTML(contents : ByteArray) =
                ok(contents, listOf(ContentType.TEXT_HTML.value))

        /**
         * If you are responding with a success message and it is CSS
         */
        private fun okCSS(contents : ByteArray) =
                ok(contents, listOf(ContentType.TEXT_CSS.value, caching))
        /**
         * If you are responding with a success message and it is JavaScript
         */
        private fun okJS (contents : ByteArray) =
                ok(contents, listOf(ContentType.APPLICATION_JAVASCRIPT.value, caching))

        private fun okJPG (contents : ByteArray) =
            ok(contents, listOf(ContentType.IMAGE_JPEG.value, caching))

        private fun okWEBP (contents : ByteArray) =
                ok(contents, listOf(ContentType.IMAGE_WEBP.value, caching))

        private fun ok (contents: ByteArray, ct : List<String>) =
                PreparedResponseData(contents, StatusCode.OK, ct)

        /**
         * Use this to redirect to any particular page
         */
        fun redirectTo(path: String): PreparedResponseData {
            return PreparedResponseData("", StatusCode.SEE_OTHER, listOf("Location: $path"))
        }

        /**
         * sends data as the body of a response from server
         * Also adds necessary across-the-board headers
         */
        fun returnData(server: ISocketWrapper, data: PreparedResponseData) {
            logTrace { "Assembling data just before shipping to client" }
            val status = "HTTP/1.1 ${data.statusCode.value}"
            logTrace { "status: $status" }
            val basicServerHeaders = generateBasicServerResponseHeaders(data)
            val allHeaders = basicServerHeaders + data.headers

            val headerResponse = "$status$CRLF" +
                    allHeaders.joinToString(CRLF) +
                    CRLF +
                    CRLF

            server.write(headerResponse)
            server.writeBytes(data.fileContents)
        }

        private fun generateBasicServerResponseHeaders(data: PreparedResponseData): List<String> {
            return listOf(
                "$CONTENT_LENGTH: ${data.fileContents.size}",

                // security-oriented headers
                "X-Frame-Options: DENY",
                "X-Content-Type-Options: nosniff",
                "server: cerver",
            )
        }

    }
}