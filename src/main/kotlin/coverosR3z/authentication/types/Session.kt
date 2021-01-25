package coverosR3z.authentication.types

import coverosR3z.misc.types.DateTime
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.misc.utility.checkParseToLong
import coverosR3z.misc.utility.decode
import coverosR3z.misc.utility.encode
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.deserializer

/**
 * This stores the information about when a user successfully logged
 * into the system.
 * @param sessionId the text identifier given to the user as a cookie, like "abc123",
 *        usually in a form like this: cookie: sessionId=abc123
 * @param user the user who is logged in
 * @param dt the date and time the user successfully logged in
 */
data class Session(val simpleId: Int, val sessionId: String, val user: User, val dt: DateTime) : IndexableSerializable {

    override fun getIndex(): Int {
        return simpleId
    }

    override fun serialize(): String {
        return """{ sid: $simpleId , s: ${encode(sessionId)} , id: ${user.id.value} , e: ${dt.epochSecond} }"""
    }

    companion object {

        fun deserialize(str: String, users: Set<User>) : Session {
            return deserializer(str, Session::class.java) { groups ->
                val simpleId = checkParseToInt(groups[1])
                val sessionString = decode(groups[3])
                val id = checkParseToInt(groups[5])
                val epochSecond = checkParseToLong(groups[7])
                val user = try {
                    users.single { it.id.value == id }
                } catch (ex : NoSuchElementException) {
                    throw DatabaseCorruptedException("Unable to find a user with the id of $id.  User set size: ${users.size}")
                }
                Session(simpleId, sessionString, user, DateTime(epochSecond))
            }
        }
    }

}
