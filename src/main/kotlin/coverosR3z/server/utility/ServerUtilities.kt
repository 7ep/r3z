package coverosR3z.server.utility

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.logging.ILogger
import coverosR3z.misc.utility.toBytes
import coverosR3z.server.api.handleBadRequest
import coverosR3z.server.api.handleInternalServerError
import coverosR3z.server.types.*
import java.net.SocketTimeoutException


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

class ServerUtilities() {
    companion object {

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
            return PreparedResponseData("", StatusCode.SEE_OTHER, listOf("Location: $path"))
        }

        /**
         * sends data as the body of a response from server
         * Also adds necessary across-the-board headers
         */
        fun returnData(server: ISocketWrapper, data: PreparedResponseData, logger: ILogger) {
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

                // security-oriented headers
                "X-Frame-Options: DENY",
                "X-Content-Type-Options: nosniff",
                "server: cerver",
            )
        }

        fun processConnectedClient(
            server: SocketWrapper,
            businessCode: BusinessCode,
            serverObjects: ServerObjects,
        ) {
            val logger = serverObjects.logger
            logger.logTrace { "client from ${server.socket.inetAddress?.hostAddress} has connected" }
            var shouldKeepAlive : Boolean
            do try {
                val requestData = handleRequest(server, businessCode, serverObjects)
                shouldKeepAlive =
                    requestData.headers.any { it.toLowerCase().contains("connection: keep-alive") }
                if (shouldKeepAlive) {
                    logger.logTrace { "This is a keep-alive connection" }
                }
            } catch (ex : SocketTimeoutException) {
                // we get here if we wait too long on reading from the socket
                // without getting anything.  See SocketWrapper and soTimeout
                logger.logTrace { "read timed out" }
                shouldKeepAlive = false
            }
            while (shouldKeepAlive)

            logger.logTrace { "closing server socket" }
            server.close()
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
                        RoutingUtilities.routeToEndpoint(
                            ServerData(
                                businessCode.au,
                                truWithUser,
                                analyzedHttpData,
                                AuthUtilities.isAuthenticated(analyzedHttpData.user),
                                serverObjects.logger
                            )
                        )
                    }
                }
            } catch (ex : SocketTimeoutException) {
                throw ex
            }
            catch (ex: Exception) {
                // If there ane any complaints whatsoever, we return them here
                handleInternalServerError(ex.message ?: ex.stackTraceToString(), ex.stackTraceToString(), serverObjects.logger)
            }

            returnData(server, responseData, serverObjects.logger)
            return analyzedHttpData
        }


    }
}