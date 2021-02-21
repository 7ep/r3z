package coverosR3z.server.utility

import coverosR3z.FullSystem
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.logging.ILogger
import coverosR3z.logging.ILogger.Companion.logImperative
import coverosR3z.misc.utility.toBytes
import coverosR3z.server.api.handleBadRequest
import coverosR3z.server.api.handleInternalServerError
import coverosR3z.server.types.*
import coverosR3z.server.types.CacheControl.*
import coverosR3z.server.types.Pragma.*
import java.lang.IllegalStateException
import java.net.ServerSocket
import java.net.SocketException
import java.net.SocketTimeoutException
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

        fun createServerThread(executorService: ExecutorService, fullSystem: FullSystem, halfOpenServerSocket: ServerSocket, businessObjects : BusinessCode, serverObjects: ServerObjects) : Thread {
            return Thread {
                try {
                    while (true) {
                        fullSystem.logger.logTrace { "waiting for socket connection" }
                        val server = SocketWrapper(halfOpenServerSocket.accept(), "server", fullSystem)
                        executorService.submit(Thread { processConnectedClient(server, businessObjects, serverObjects) })
                    }
                } catch (ex: SocketException) {
                    if (ex.message == "Interrupted function call: accept failed") {
                        logImperative("Server was shutdown while waiting on accept")
                    }
                }
            }
        }

        /**
         * This is here just to provide a behavior to redirect whatever
         * we receive to the SSL equivalent
         */
        fun createRedirectingServerThread(executorService: ExecutorService, fullSystem: FullSystem, halfOpenServerSocket: ServerSocket, businessObjects : BusinessCode, serverObjects: ServerObjects) : Thread {
            return Thread {
                try {
                    while (true) {
                        fullSystem.logger.logTrace { "waiting for socket connection" }
                        val server = SocketWrapper(halfOpenServerSocket.accept(), "server", fullSystem)
                        executorService.submit(Thread { redirectClientToSSLEndpoint(server, businessObjects, serverObjects) })
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
            return listOf(
                "$CONTENT_LENGTH: ${data.fileContents.size}",

                // this disallows rendering the page in a frame
                // helps avoid click-jack attacks.
                // See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options
                "X-Frame-Options: DENY",

                // see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options
                "X-Content-Type-Options: nosniff",

                // The name we advertise for our server
                "server: R3z",
            )
        }

        private fun processConnectedClient(
            server: SocketWrapper,
            businessCode: BusinessCode,
            serverObjects: ServerObjects,
        ) {
            val logger = serverObjects.logger
            logger.logTrace { "client from ${server.socket.inetAddress?.hostAddress} has connected" }
            var shouldKeepAlive : Boolean
            try {
                do {
                    val requestData = handleRequest(server, businessCode, serverObjects)
                    shouldKeepAlive =
                        requestData.headers.any { it.toLowerCase().contains("connection: keep-alive") }
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
                    throw ex
                }
            } finally {
                logger.logTrace { "closing server socket" }
                server.close()
            }
        }

        /**
         * If the client is coming in on the insecure port,
         * redirect them to the secure port
         */
        private fun redirectClientToSSLEndpoint(
            server: SocketWrapper,
            businessCode: BusinessCode,
            serverObjects: ServerObjects,
        ) {
            val logger = serverObjects.logger
            logger.logTrace { "client from ${server.socket.inetAddress?.hostAddress} has connected" }
            try {
                handleRedirectionToSslEndpoint(server, businessCode, serverObjects)
            } catch (ex : SocketTimeoutException) {
                // we get here if we wait too long on reading from the socket
                // without getting anything.  See SocketWrapper and soTimeout
                logger.logTrace { "read timed out" }
            } catch (ex : SocketException) {
                if (ex.message?.contains("Connection reset") == true) {
                    logger.logTrace { "client closed their connection while we were waiting to read from the socket" }
                } else {
                    throw ex
                }
            } finally {
                logger.logTrace { "closing server socket" }
                server.close()
            }
        }

        private fun handleRequest(server: ISocketWrapper, businessCode: BusinessCode, serverObjects: ServerObjects) : AnalyzedHttpData {
            var analyzedHttpData = AnalyzedHttpData()
            val responseData: PreparedResponseData = try {
                analyzedHttpData = parseHttpMessage(server, businessCode.au, serverObjects.logger)

                serverObjects.logger.logDebug{ "client requested ${analyzedHttpData.verb} /${analyzedHttpData.path}" }
                if (analyzedHttpData.verb == Verb.CLIENT_CLOSED_CONNECTION) {
                    return analyzedHttpData
                }

                if (analyzedHttpData.verb == Verb.INVALID) {
                    handleBadRequest()
                } else {
                    // if we can just return a static file now, do that...
                    val staticResponse : PreparedResponseData? = serverObjects.staticFileCache[analyzedHttpData.path]
                    if (staticResponse != null) {
                        staticResponse
                    } else {
                        // otherwise review the routing
                        // now that we know who the user is (if they authenticated) we can update the current user
                        val truWithUser = businessCode.tru.changeUser(CurrentUser(analyzedHttpData.user))
                        val dynamicResponse = RoutingUtilities.routeToEndpoint(
                            ServerData(
                                businessCode.au,
                                truWithUser,
                                analyzedHttpData,
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
            } catch (ex : SocketTimeoutException) {
                throw ex
            } catch (ex : SocketException) {
                throw ex
            } catch (ex: Exception) {
                // If there ane any complaints whatsoever, we return them here
                handleInternalServerError(ex.message ?: ex.stackTraceToString(), ex.stackTraceToString(), serverObjects.logger)
            }

            returnData(server, responseData, serverObjects.logger)
            return analyzedHttpData
        }

        /**
         * if the user is coming in on the insecure connection (http), do some
         * checking like ordinary but then immediately redirect them to
         * the secure server
         */
        private fun handleRedirectionToSslEndpoint(server: ISocketWrapper, businessCode: BusinessCode, serverObjects: ServerObjects) : AnalyzedHttpData {
            var analyzedHttpData = AnalyzedHttpData()
            val responseData: PreparedResponseData = try {
                analyzedHttpData = parseHttpMessage(server, businessCode.au, serverObjects.logger)

                serverObjects.logger.logDebug{ "client requested ${analyzedHttpData.verb} /${analyzedHttpData.path}" }
                if (analyzedHttpData.verb == Verb.CLIENT_CLOSED_CONNECTION) {
                    return analyzedHttpData
                }

                if (analyzedHttpData.verb == Verb.INVALID) {
                    handleBadRequest()
                } else {
                    try {
                        val hostHeader = analyzedHttpData.headers.single { it.toLowerCase().startsWith("host") }
                        val extractedHost = extractHostFromHostHeader(hostHeader)
                        val rawQueryString = analyzedHttpData.rawQueryString
                        val queryString = if (rawQueryString.isNotBlank()) { "?" + rawQueryString } else ""
                        redirectTo("https://" + extractedHost + ":" + serverObjects.sslPort + "/" + analyzedHttpData.path + queryString)
                    } catch (ex: Exception) {
                        handleBadRequest()
                    }
                }
            } catch (ex : SocketTimeoutException) {
                throw ex
            } catch (ex : SocketException) {
                throw ex
            } catch (ex: Exception) {
                // If there ane any complaints whatsoever, we return them here
                handleInternalServerError(ex.message ?: ex.stackTraceToString(), ex.stackTraceToString(), serverObjects.logger)
            }

            returnData(server, responseData, serverObjects.logger)
            return analyzedHttpData
        }

        private fun extractHostFromHostHeader(hostHeader: String): String {
            val notMatchingMessage = "The host header we received did not match the expected pattern. It was: $hostHeader"
            check (hostHeaderRegex.matches(hostHeader)) { notMatchingMessage }

            return hostHeaderRegex.matchEntire(hostHeader)?.groupValues?.get(1) ?: throw IllegalStateException(notMatchingMessage)
        }


    }
}