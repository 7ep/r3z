package coverosR3z

import coverosR3z.logging.logTrace
import coverosR3z.misc.utility.FileReader
import coverosR3z.misc.utility.toStr
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL

class BDDHelpers(private var filename : String) {

    private var fileContents: String
    private val sourceDirectory = "BDD/"
    private val destinationDirectory = "build/reports/BDD/"

    init {
        // if the destination already has this file, we'll use it
        val fileContentsAtDestination = try {File("$destinationDirectory$filename").readBytes()} catch (ex : FileNotFoundException) {null}

        // but if it's not there, just use the one in the resources directory
        fileContents = toStr(fileContentsAtDestination ?: FileReader.read("$sourceDirectory$filename")!!)
    }

    /**
     * In the BDD file, replace "not-done" with "done" if we have done that
     * step in the scenario
     */
    fun markDone(s: String) {
        fileContents = fileContents.replace("<not-done>$s</not-done>", "<done>$s</done>")
    }

    /**
     * When we're finished with our tests, write the changed file to its proper location
     */
    fun writeToFile() {
        File(destinationDirectory).mkdirs()
        File("$destinationDirectory$filename").writeText(fileContents)
        copyPersonasToReportsDirectory()
        copyStylesheet()
    }

    private fun copyPersonasToReportsDirectory() {
        val originalLocation: URL = this::class.java.classLoader.getResource("${sourceDirectory}personas/")!!
        val newLocation = File("${destinationDirectory}personas")
        newLocation.mkdirs()
        File(originalLocation.file + "/")
            .copyRecursively(
                newLocation,
                onError = {file: File, ioException: IOException ->
                    if (ioException is FileAlreadyExistsException) {
                        logTrace { "skipping file $file, already exists at destination" }
                        OnErrorAction.SKIP
                    } else {
                        throw ioException
                    }})
    }

    private fun copyStylesheet() {
        val originalLocation: URL = this::class.java.classLoader.getResource("${sourceDirectory}BDD.css")!!
        val destination = File(destinationDirectory + "BDD.css")
        if (! destination.exists()) {
            File(originalLocation.file).copyTo(destination)
        }
    }

}