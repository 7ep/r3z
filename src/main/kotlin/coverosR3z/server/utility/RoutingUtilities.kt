package coverosR3z.server.utility

import coverosR3z.authentication.api.LoginAPI
import coverosR3z.authentication.api.LogoutAPI
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.logging.LoggingAPI
import coverosR3z.server.api.HomepageAPI
import coverosR3z.server.api.handleNotFound
import coverosR3z.server.types.NamedPaths
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.Verb
import coverosR3z.timerecording.api.*


/**
 * Examine the request and headers, direct the request to a proper
 * point in the system that will take the proper action, returning a
 * proper response with headers.
 *
 * If we cannot find a dynamic processor, it means the user wants a static
 * file, which we handle at the end.
 *
 */
fun routeToEndpoint(sd : ServerData): PreparedResponseData {
    return when (Pair(sd.ahd.verb, sd.ahd.path)){
        // GET

        Pair(Verb.GET, ""),
        Pair(Verb.GET, NamedPaths.HOMEPAGE.path)  -> HomepageAPI.handleGet(sd)
        Pair(Verb.GET, NamedPaths.ENTER_TIME.path) -> EnterTimeAPI.handleGet(sd)
        Pair(Verb.GET, NamedPaths.TIMEENTRIES.path) -> ViewTimeAPI.handleGet(sd)
        Pair(Verb.GET, NamedPaths.CREATE_EMPLOYEE.path) -> CreateEmployeeAPI.handleGet(sd)
        Pair(Verb.GET, NamedPaths.EMPLOYEES.path) -> ViewEmployeesAPI.handleGet(sd)
        Pair(Verb.GET, NamedPaths.LOGIN.path) -> LoginAPI.handleGet(sd)
        Pair(Verb.GET, NamedPaths.REGISTER.path) -> RegisterAPI.handleGet(sd)
        Pair(Verb.GET, NamedPaths.CREATE_PROJECT.path) -> ProjectAPI.handleGet(sd)
        Pair(Verb.GET, NamedPaths.LOGOUT.path) -> LogoutAPI.handleGet(sd)
        Pair(Verb.GET, NamedPaths.LOGGING.path) -> LoggingAPI.handleGet(sd)

        // POST

        Pair(Verb.POST, NamedPaths.ENTER_TIME.path) -> EnterTimeAPI.handlePost(sd)
        Pair(Verb.POST, NamedPaths.CREATE_EMPLOYEE.path) -> CreateEmployeeAPI.handlePost(sd)
        Pair(Verb.POST, NamedPaths.LOGIN.path) -> LoginAPI.handlePost(sd)
        Pair(Verb.POST, NamedPaths.REGISTER.path) -> RegisterAPI.handlePost(sd)
        Pair(Verb.POST, NamedPaths.CREATE_PROJECT.path) -> ProjectAPI.handlePost(sd)
        Pair(Verb.POST, NamedPaths.LOGGING.path) -> LoggingAPI.handlePost(sd)

        else -> {
            handleNotFound()
        }
    }
}