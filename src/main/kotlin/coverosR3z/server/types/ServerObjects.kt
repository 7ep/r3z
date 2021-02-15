package coverosR3z.server.types

import coverosR3z.logging.ILogger
import coverosR3z.logging.Logger

/**
 * Data needed by a server that isn't business-related
 */
class ServerObjects(val staticFileCache: Map<String, PreparedResponseData>, val logger: ILogger)