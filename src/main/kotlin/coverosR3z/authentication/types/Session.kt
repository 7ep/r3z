package coverosR3z.authentication.types

import coverosR3z.misc.types.DateTime
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.misc.utility.checkParseToLong
import coverosR3z.misc.utility.decode
import coverosR3z.misc.utility.encode
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.persistence.types.Deserializable
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializationKeys
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.deserialize

/**
 * This stores the information about when a user successfully logged
 * into the system.
 * @param sessionId the text identifier given to the user as a cookie, like "abc123",
 *        usually in a form like this: cookie: sessionId=abc123
 * @param user the user who is logged in
 * @param dt the date and time the user successfully logged in
 */
data class Session(val simpleId: Int, val sessionId: String, val user: User, val dt: DateTime) : IndexableSerializable() {

    override fun getIndex(): Int {
        return simpleId
    }

    override val dataMappings: Map<String, String>
        get() = mapOf(
            Keys.SIMPLE_ID.getKey() to "$simpleId",
            Keys.SESSION_ID.getKey() to encode(sessionId),
            Keys.USER_ID.getKey() to "${user.id.value}",
            Keys.EPOCH_SECOND.getKey() to "${dt.epochSecond}"
        )

    class Deserializer(val users: Set<User>) : Deserializable<Session> {

        override fun deserialize(str: String): Session {
            return deserialize(str, Session::class.java) { entries ->
                val simpleId = checkParseToInt(entries[Keys.SIMPLE_ID.getKey()])
                val sessionString = decode(entries[Keys.SESSION_ID.getKey()])
                val id = checkParseToInt(entries[Keys.USER_ID.getKey()])
                val epochSecond = checkParseToLong(entries[Keys.EPOCH_SECOND.getKey()])
                val user = try {
                    users.single { it.id.value == id }
                } catch (ex: NoSuchElementException) {
                    throw DatabaseCorruptedException("Unable to find a user with the id of $id.  User set size: ${users.size}")
                }
                Session(simpleId, sessionString, user, DateTime(epochSecond))
            }
        }
    }

    companion object {

        enum class Keys(private val keyString: String) : SerializationKeys {
            SIMPLE_ID("sid"),
            SESSION_ID("s"),
            USER_ID("id"),
            EPOCH_SECOND("e");

            /**
             * This needs to be a method and not just a value of the class
             * so that we can have it meet an interface specification, so
             * that we can use it in generic code
             */
            override fun getKey() : String {
                return keyString
            }
        }

    }

}
