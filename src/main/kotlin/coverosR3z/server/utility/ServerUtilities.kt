package coverosR3z.server.utility

import coverosR3z.authentication.api.LoginAPI
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.api.generateLogoutPage
import coverosR3z.logging.LoggingAPI
import coverosR3z.logging.logImperative
import coverosR3z.logging.logTrace
import coverosR3z.misc.utility.FileReader
import coverosR3z.misc.utility.toBytes
import coverosR3z.server.api.HomepageAPI
import coverosR3z.server.api.handleBadRequest
import coverosR3z.server.api.handleNotFound
import coverosR3z.server.types.*
import coverosR3z.server.utility.NamedPaths.*
import coverosR3z.timerecording.api.EmployeeAPI
import coverosR3z.timerecording.api.EnterTimeAPI
import coverosR3z.timerecording.api.ProjectAPI
import coverosR3z.timerecording.api.ViewTimeAPI
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
const val CONTENT_LENGTH = "content-length"
const val maxContentLength = 400_000

val caching = CacheControl.AGGRESSIVE_WEB_CACHE.details

/**
 * Examine the request and headers, direct the request to a proper
 * point in the system that will take the proper action, returning a
 * proper response with headers.
 *
 * If we cannot find a dynamic processor, it means the user wants a static
 * file, which we handle at the end.
 *
 */
fun directToProcessor(sd : ServerData): PreparedResponseData {
    val verb = sd.rd.verb
    val path = sd.rd.path
    val au = sd.au
    val tru = sd.tru
    val rd = sd.rd

    /**
     * The user currently logged in
     */
    val user = rd.user

    /**
     * The data sent in the POST body
     */
    val data = rd.data

    if (verb == Verb.INVALID) {
        return handleBadRequest()
    }

    val authStatus = sd.authStatus

    return when (Pair(verb, path)){
        Pair(Verb.GET, ""),
        Pair(Verb.GET, HOMEPAGE.path)  -> HomepageAPI.handleGet(sd)
        Pair(Verb.GET, ENTER_TIME.path) -> doGETRequireAuth(authStatus) { EnterTimeAPI.generateEnterTimePage(tru, user.name) }
        Pair(Verb.GET, TIMEENTRIES.path) -> ViewTimeAPI.handleGet(sd)
        Pair(Verb.GET, CREATE_EMPLOYEE.path) -> doGETRequireAuth(authStatus) { EmployeeAPI.generateCreateEmployeePage(user.name) }
        Pair(Verb.GET, EMPLOYEES.path) -> doGETRequireAuth(authStatus) { EmployeeAPI.generateExistingEmployeesPage(user.name, tru) }
        Pair(Verb.GET, LOGIN.path) -> doGETRequireUnauthenticated(authStatus) { LoginAPI.generateLoginPage() }
        Pair(Verb.GET, REGISTER.path) -> doGETRequireUnauthenticated(authStatus) { RegisterAPI.generateRegisterUserPage(tru) }
        Pair(Verb.GET, CREATE_PROJECT.path) -> doGETRequireAuth(authStatus) { ProjectAPI.generateCreateProjectPage(user.name) }
        Pair(Verb.GET, LOGOUT.path) -> doGETRequireAuth(authStatus) { generateLogoutPage(au, user) }
        Pair(Verb.GET, LOGGING.path) -> doGETRequireAuth(authStatus) { LoggingAPI.generateLoggingConfigPage() }

        // posts
        Pair(Verb.POST, ENTER_TIME.path) -> doPOSTAuthenticated(authStatus, EnterTimeAPI.requiredInputs, data) { EnterTimeAPI.handlePOST(tru, user.employeeId, data) }
        Pair(Verb.POST, CREATE_EMPLOYEE.path) -> doPOSTAuthenticated(authStatus, EmployeeAPI.requiredInputs, data) { EmployeeAPI.handlePOST(tru, data) }
        Pair(Verb.POST, LOGIN.path) -> doPOSTRequireUnauthenticated(authStatus, LoginAPI.requiredInputs, data) { LoginAPI.handlePOST(au, data) }
        Pair(Verb.POST, REGISTER.path) -> doPOSTRequireUnauthenticated(authStatus, RegisterAPI.requiredInputs, data) { RegisterAPI.handlePOST(au, data) }
        Pair(Verb.POST, CREATE_PROJECT.path) -> doPOSTAuthenticated(authStatus, ProjectAPI.requiredInputs, data) { ProjectAPI.handlePOST(tru, data) }
        Pair(Verb.POST, LOGGING.path) -> doPOSTAuthenticated(authStatus, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }

        else -> {
            handleNotFound()
        }
    }
}

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
    val urls = checkNotNull(FileReader.getResources("static/"))
    for (url in urls) {
        val uri = url.toURI()

        val myPath = if (uri.scheme == "jar") {
            val fileSystem: FileSystem = FileSystems.newFileSystem(uri, emptyMap<String, Any>())
            fileSystem.getPath("static/")
        } else {
            Paths.get(uri)
        }

        for (path: Path in Files.walk(myPath, 1)) {
            val fileContents = FileReader.read("static/" + path.fileName.toString()) ?: continue
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
fun okCSS(contents : ByteArray) =
        ok(contents, listOf(ContentType.TEXT_CSS.value, caching))
/**
 * If you are responding with a success message and it is JavaScript
 */
fun okJS (contents : ByteArray) =
        ok(contents, listOf(ContentType.APPLICATION_JAVASCRIPT.value, caching))

fun okJPG (contents : ByteArray) =
    ok(contents, listOf(ContentType.IMAGE_JPEG.value, caching))

fun okWEBP (contents : ByteArray) =
        ok(contents, listOf(ContentType.IMAGE_WEBP.value, caching))

private fun ok (contents: ByteArray, ct : List<String>) =
        PreparedResponseData(contents, StatusCode.OK, ct)

/**
 * Use this to redirect to any particular page
 */
fun redirectTo(path: String): PreparedResponseData {
    return PreparedResponseData("", StatusCode.SEE_OTHER, listOf(ContentType.TEXT_HTML.value, "Location: $path"))
}

/**
 * sends data as the body of a response from server
 */
fun returnData(server: ISocketWrapper, data: PreparedResponseData) {
    logTrace { "Assembling data just before shipping to client" }
    val status = "HTTP/1.1 ${data.statusCode.value}"
    logTrace { "status: $status" }
    val contentLengthHeader = "Content-Length: ${data.fileContents.size}"

    val otherHeaders = data.headers.joinToString(CRLF)
    logTrace { "contentLengthHeader: $contentLengthHeader" }
    data.headers.forEach{ logTrace { "sending header: $it" } }
    server.write("$status$CRLF" +
            "$contentLengthHeader$CRLF" +
            otherHeaders +
            CRLF +
            CRLF
    )
    server.writeBytes(data.fileContents)
}

