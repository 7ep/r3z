package coverosR3z.authentication.types

import coverosR3z.misc.types.DateTime
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.misc.utility.checkParseToLong
import coverosR3z.misc.utility.decode
import coverosR3z.misc.utility.encode
import coverosR3z.misc.types.Serializable
import coverosR3z.persistence.utility.PureMemoryDatabase.Companion.deserializer

/**
 * This stores the information about when a user successfully logged
 * into the system.
 * @param sessionId the text identifier given to the user as a cookie, like "abc123",
 *        usually in a form like this: cookie: sessionId=abc123
 * @param user the user who is logged in
 * @param dt the date and time the user successfully logged in
 */
data class Session(val sessionId: String, val user: User, val dt: DateTime) : Serializable {

    override fun serialize(): String {
        return """{ s: ${encode(sessionId)} , id: ${user.id.value} , e: ${dt.epochSecond} }"""
    }

    companion object {

        fun deserialize(str: String, users: Set<User>) : Session {
            return deserializer(str, Session::class.java) { groups ->
                val sessionString = decode(groups[1])
                val id = checkParseToInt(groups[3])
                val epochSecond = checkParseToLong(groups[5])
                val user = try {
                    users.single { it.id.value == id }
                } catch (ex : NoSuchElementException) {
                    throw DatabaseCorruptedException("Unable to find a user with the id of $id.  User set size: ${users.size}")
                }
                Session(sessionString, user, DateTime(epochSecond))
            }
        }
    }

}
