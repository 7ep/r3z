package coverosR3z.server.types

import coverosR3z.logging.ILogger

/**
 * Data needed by a server that isn't business-related
 */
class ServerObjects(
    val staticFileCache: Map<String, PreparedResponseData>,
    val logger: ILogger,
    )