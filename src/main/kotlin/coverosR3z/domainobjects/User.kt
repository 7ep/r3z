package coverosR3z.domainobjects

import kotlinx.serialization.Serializable

@Serializable
data class User(val id : Int, val name : String, val hash : String)