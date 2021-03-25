package coverosR3z.server.types

import coverosR3z.logging.ILogger
import coverosR3z.system.persistence.ISystemConfigurationPersistence

/**
 * Data needed by a server that isn't business-related
 */
data class ServerObjects(
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
    val allowInsecureUsage: Boolean,

    /**
     * This is extracted from the Host header we receive from the browser.
     * Call us whatever you want, as long as you don't call us late for supper.
     */
    val host: String = "",

    /**
     * This is used to store changes to the system's
     * configuration that may take place during run-time,
     * for example, the logging settings.
     */
    val scp: ISystemConfigurationPersistence,
    )