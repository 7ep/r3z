package coverosR3z.server.types

import coverosR3z.logging.ILogger

/**
 * Data needed by a server that isn't business-related
 */
class ServerObjects(
    val staticFileCache: Map<String, PreparedResponseData>,
    val logger: ILogger,
    /**
     * Regular non-secure port for the server
     */
    val port: Int,
    val sslPort: Int,

    /**
     * If this is true, we allow clients to remain on the
     * non-secure http endpoint without redirecting them
     * to the https secure port
     */
    val allowInsecureUsage: Boolean
    )