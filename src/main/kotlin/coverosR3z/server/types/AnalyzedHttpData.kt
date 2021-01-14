package coverosR3z.server.types

import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.User

/**
 * Encapsulates the proper action by the server, based on what
 * the client wants from us
 */
data class AnalyzedHttpData(
    val verb: Verb = Verb.NONE,
    val path: String = "(NOTHING REQUESTED)",
    val data: PostBodyData = PostBodyData(),
    val user: User = NO_USER,
    val sessionToken: String = "NO TOKEN",
    val headers: List<String> = emptyList(),
    val statusCode: StatusCode = StatusCode.NONE
)