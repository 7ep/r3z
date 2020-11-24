package coverosR3z.domainobjects

import kotlinx.serialization.Serializable

@Serializable
data class Session(val user: User, val dt: DateTime)
