package coverosR3z.server.utility

import coverosR3z.config.STATIC_FILES_DIRECTORY
import coverosR3z.logging.Logger
import coverosR3z.misc.utility.FileReader
import coverosR3z.server.api.handleNotFound
import coverosR3z.server.types.PreparedResponseData
import java.nio.file.*

class StaticFilesUtilities {
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
            Logger.logImperative("Loading all static files into cache")

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
                            filename.takeLast(4) == ".css" -> ServerUtilities.okCSS(fileContents)
                            filename.takeLast(3) == ".js" -> ServerUtilities.okJS(fileContents)
                            filename.takeLast(4) == ".jpg" -> ServerUtilities.okJPG(fileContents)
                            filename.takeLast(5) == ".webp" -> ServerUtilities.okWEBP(fileContents)
                            filename.takeLast(5) == ".html" || filename.takeLast(4) == ".htm" -> ServerUtilities.okHTML(
                                fileContents
                            )
                            else -> handleNotFound()
                        }

                    cache[filename] = result
                }
            }
        }

    }
}