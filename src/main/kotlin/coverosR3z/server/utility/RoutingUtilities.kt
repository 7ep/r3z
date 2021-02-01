package coverosR3z.server.utility

import coverosR3z.authentication.api.LoginAPI
import coverosR3z.authentication.api.LogoutAPI
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.logging.LoggingAPI
import coverosR3z.server.api.HomepageAPI
import coverosR3z.server.api.handleNotFound
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.Verb
import coverosR3z.timerecording.api.*


class RoutingUtilities {

    companion object {
        /**
         * Examine the request and headers, direct the request to a proper
         * point in the system that will take the proper action, returning a
         * proper response with headers.
         *
         * Register your endpoints here
         */
        fun routeToEndpoint(sd: ServerData): PreparedResponseData {
            return when (Pair(sd.ahd.verb, sd.ahd.path)) {
                // GET

                Pair(Verb.GET, ""),
                Pair(Verb.GET, HomepageAPI.path) -> HomepageAPI.handleGet(sd)
                Pair(Verb.GET, EnterTimeAPI.path) -> EnterTimeAPI.handleGet(sd)
                Pair(Verb.GET, ViewTimeAPI.path) -> ViewTimeAPI.handleGet(sd)
                Pair(Verb.GET, CreateEmployeeAPI.path) -> CreateEmployeeAPI.handleGet(sd)
                Pair(Verb.GET, ViewEmployeesAPI.path) -> ViewEmployeesAPI.handleGet(sd)
                Pair(Verb.GET, LoginAPI.path) -> LoginAPI.handleGet(sd)
                Pair(Verb.GET, RegisterAPI.path) -> RegisterAPI.handleGet(sd)
                Pair(Verb.GET, ProjectAPI.path) -> ProjectAPI.handleGet(sd)
                Pair(Verb.GET, LogoutAPI.path) -> LogoutAPI.handleGet(sd)
                Pair(Verb.GET, LoggingAPI.path) -> LoggingAPI.handleGet(sd)
                Pair(Verb.GET, TimeEntryMobileAPI.path) -> TimeEntryMobileAPI.handleGet(sd)

                // POST

                Pair(Verb.POST, EnterTimeAPI.path) -> EnterTimeAPI.handlePost(sd)
                Pair(Verb.POST, CreateEmployeeAPI.path) -> CreateEmployeeAPI.handlePost(sd)
                Pair(Verb.POST, LoginAPI.path) -> LoginAPI.handlePost(sd)
                Pair(Verb.POST, RegisterAPI.path) -> RegisterAPI.handlePost(sd)
                Pair(Verb.POST, ProjectAPI.path) -> ProjectAPI.handlePost(sd)
                Pair(Verb.POST, LoggingAPI.path) -> LoggingAPI.handlePost(sd)
                Pair(Verb.POST, ViewTimeAPI.path) -> ViewTimeAPI.handlePost(sd)

                else -> {
                    handleNotFound()
                }
            }
        }
    }
}