package coverosR3z.server.utility

import coverosR3z.FullSystem
import coverosR3z.FullSystem.Companion.initializeBusinessCode
import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.logging.ILogger
import coverosR3z.logging.ILogger.Companion.logImperative
import coverosR3z.misc.utility.toBytes
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.api.handleBadRequest
import coverosR3z.server.api.handleInternalServerError
import coverosR3z.server.types.*
import coverosR3z.server.types.CacheControl.AGGRESSIVE_WEB_CACHE
import coverosR3z.server.types.CacheControl.DISALLOW_CACHING
import coverosR3z.server.types.Pragma.PRAGMA_DISALLOW_CACHING
import java.net.ServerSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService


/**
 *   HTTP/1.1 defines the sequence CR LF as the end-of-line marker for all
 *  protocol elements except the entity-body (see appendix 19.3 for
 *  tolerant applications). The end-of-line marker within an entity-body
 *  is defined by its associated media type, as described in section 3.7.
 *
 *  See https://tools.ietf.org/html/rfc2616
 */
const val CRLF = "\r\n"

class ServerUtilities {
    companion object {

        fun createServerThread(
            executorService: ExecutorService,
            fullSystem: FullSystem,
            halfOpenServerSocket: ServerSocket,
            serverObjects: ServerObjects) : Thread {
            return Thread {
                try {
                    while (true) {
                        fullSystem.logger.logTrace { "waiting for socket connection" }
                        val server = SocketWrapper(halfOpenServerSocket.accept(), "server", fullSystem)
                        executorService.submit(Thread { processConnectedClient(server, fullSystem.pmd, serverObjects) })
                    }
                } catch (ex: SocketException) {
                    if (ex.message == "Interrupted function call: accept failed") {
                        logImperative("Server was shutdown while waiting on accept")
                    }
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
                ok(contents, listOf(ContentType.TEXT_CSS.value, AGGRESSIVE_WEB_CACHE.details))
        /**
         * If you are responding with a success message and it is JavaScript
         */
        fun okJS (contents : ByteArray) =
                ok(contents, listOf(ContentType.APPLICATION_JAVASCRIPT.value, AGGRESSIVE_WEB_CACHE.details))

        fun okJPG (contents : ByteArray) =
            ok(contents, listOf(ContentType.IMAGE_JPEG.value, AGGRESSIVE_WEB_CACHE.details))

        fun okWEBP (contents : ByteArray) =
                ok(contents, listOf(ContentType.IMAGE_WEBP.value, AGGRESSIVE_WEB_CACHE.details))

        /**
         * @param extraHeaders any extra headers to send
         */
        private fun ok (contents: ByteArray, extraHeaders : List<String>) =
                PreparedResponseData(contents, StatusCode.OK, extraHeaders)

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
        private fun returnData(server: ISocketWrapper, data: PreparedResponseData, logger: ILogger) {
            logger.logTrace { "Assembling data just before shipping to client" }
            val status = "HTTP/1.1 ${data.statusCode.value}"
            logger.logTrace { "status: $status" }
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
            val date = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.RFC_1123_DATE_TIME)

            return listOf(
                "$CONTENT_LENGTH: ${data.fileContents.size}",

                // this disallows rendering the page in a frame
                // helps avoid click-jack attacks.
                // See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options
                "X-Frame-Options: DENY",

                // see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options
                "X-Content-Type-Options: nosniff",

                // The name we advertise for our server
                "Server: R3z",

                // today's date and time
                "Date: $date",
            )
        }

        private fun processConnectedClient(
            server: SocketWrapper,
            pmd: PureMemoryDatabase,
            serverObjects: ServerObjects,
        ) {
            val logger = serverObjects.logger
            logger.logTrace { "client from ${server.socket.inetAddress?.hostAddress} has connected" }
            var shouldKeepAlive : Boolean
            try {
                do {
                    val requestData = handleRequest(server, pmd, serverObjects)
                    shouldKeepAlive = requestData.headers.any { it.toLowerCase().contains("connection: keep-alive") }
                    if (shouldKeepAlive) {
                        logger.logTrace { "This is a keep-alive connection" }
                    }
                } while (shouldKeepAlive)
            } catch (ex : SocketTimeoutException) {
                // we get here if we wait too long on reading from the socket
                // without getting anything.  See SocketWrapper and soTimeout
                logger.logTrace { "read timed out" }
            } catch (ex : SocketException) {
                if (ex.message?.contains("Connection reset") == true) {
                    logger.logTrace { "client closed their connection while we were waiting to read from the socket" }
                } else {
                    logger.logWarn { "${ex.message}" }
                    throw ex
                }
            } catch (ex : Throwable) {
                logger.logWarn { "${ex.message}" }
                throw ex
            } finally {
                logger.logTrace { "closing server socket" }
                server.close()
            }
        }

        private fun handleRequest(server: ISocketWrapper, pmd: PureMemoryDatabase, serverObjects: ServerObjects) : AnalyzedHttpData {
            var analyzedHttpData = AnalyzedHttpData()
            val logger = serverObjects.logger
            val responseData: PreparedResponseData = try {
                analyzedHttpData = parseHttpMessage(server, logger)

                logger.logDebug{ "client requested ${analyzedHttpData.verb} /${analyzedHttpData.path}" }
                if (analyzedHttpData.verb == Verb.CLIENT_CLOSED_CONNECTION) {
                    return analyzedHttpData
                }

                if (analyzedHttpData.verb == Verb.INVALID) {
                    handleBadRequest()
                } else {
                    // check if we are currently running on the insecure (http) endpoint
                    val onInsecureEndpoint = server.socket.localPort == serverObjects.port
                    // if the user sets the option to allow insecure use, we won't redirect to the secure endpoint
                    // but if not, redirect
                    if (!serverObjects.allowInsecureUsage && onInsecureEndpoint) {
                        analyzedHttpData = analyzedHttpData.copy(headers = analyzedHttpData.headers.filterNot {it.toLowerCase().contains("connection: keep-alive")})
                        redirectToSslEndpoint(analyzedHttpData, serverObjects)
                    } else {
                        obtainStaticAndDynamicContent(serverObjects, analyzedHttpData, pmd)
                    }
                }
            } catch (ex : SocketTimeoutException) {
                throw ex
            } catch (ex : SocketException) {
                throw ex
            }
            catch (ex: Exception) {
                // If there ane any complaints whatsoever, we return them here
                handleInternalServerError(ex.message ?: ex.stackTraceToString(), ex.stackTraceToString(), logger)
            }

            returnData(server, responseData, logger)
            return analyzedHttpData
        }

        private fun redirectToSslEndpoint(
            analyzedHttpData: AnalyzedHttpData,
            serverObjects: ServerObjects
        ): PreparedResponseData {
            return try {
                val rawQueryString = analyzedHttpData.rawQueryString
                val queryString = if (rawQueryString.isNotBlank()) {
                    "?$rawQueryString"
                } else ""
                redirectTo("https://" + serverObjects.host + ":" + serverObjects.sslPort + "/" + analyzedHttpData.path + queryString)
            } catch (ex: Exception) {
                handleBadRequest()
            }
        }

        private fun obtainStaticAndDynamicContent(
            serverObjects: ServerObjects,
            analyzedHttpData: AnalyzedHttpData,
            pmd: PureMemoryDatabase
        ): PreparedResponseData {
            // if we can just return a static file now, do that...
            val staticResponse: PreparedResponseData? = serverObjects.staticFileCache[analyzedHttpData.path]
            return if (staticResponse != null) {
                staticResponse
            } else {
                // otherwise review the routing
                // now that we know who the user is (if they authenticated) we can update the current user
                val ap = AuthenticationPersistence(pmd, serverObjects.logger)
                val user = ap.getUserForSession(analyzedHttpData.sessionToken)
                val bc = initializeBusinessCode(pmd, serverObjects.logger, CurrentUser(user))
                val ahdWithUser = analyzedHttpData.copy(user = user)

                val dynamicResponse = RoutingUtilities.routeToEndpoint(
                    ServerData(
                        bc,
                        serverObjects,
                        ahdWithUser,
                        AuthUtilities.isAuthenticated(analyzedHttpData.user),
                        serverObjects.logger,
                    )
                )

                // we'll add some headers that apply to dynamic content
                val newHeaders = mutableListOf(
                    // This is dynamically generated content, so we'll
                    // be explicit about not caching it
                    DISALLOW_CACHING.details,
                    PRAGMA_DISALLOW_CACHING.details
                )
                newHeaders.addAll(dynamicResponse.headers)

                val responseWithHeaders = dynamicResponse.copy(
                    headers = newHeaders
                )
                responseWithHeaders
            }
        }


    }
}