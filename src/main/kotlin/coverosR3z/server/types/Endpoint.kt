package coverosR3z.server.types

import coverosR3z.server.types.ServerData

interface Endpoint {
    fun respond(sd : ServerData) : PreparedResponseData
}