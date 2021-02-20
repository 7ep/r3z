package coverosR3z.authentication.types

import coverosR3z.misc.types.DateTime
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.misc.utility.checkParseToLong
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.persistence.types.Deserializable
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializableCompanion
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

    override val dataMappings: Map<SerializationKeys, String>
        get() = mapOf(
            Keys.SIMPLE_ID to "$simpleId",
            Keys.SESSION_ID to sessionId,
            Keys.USER_ID to "${user.id.value}",
            Keys.EPOCH_SECOND to "${dt.epochSecond}"
        )

    class Deserializer(val users: Set<User>) : Deserializable<Session> {

        override fun deserialize(str: String): Session {
            return deserialize(str, Companion) { entries ->
                val simpleId = checkParseToInt(entries[Keys.SIMPLE_ID])
                val sessionString = checkNotNull(entries[Keys.SESSION_ID])
                val id = checkParseToInt(entries[Keys.USER_ID])
                val epochSecond = checkParseToLong(entries[Keys.EPOCH_SECOND])
                val user = try {
                    users.single { it.id.value == id }
                } catch (ex: NoSuchElementException) {
                    throw DatabaseCorruptedException("Unable to find a user with the id of $id.  User set size: ${users.size}")
                }
                Session(simpleId, sessionString, user, DateTime(epochSecond))
            }
        }
    }

    companion object : SerializableCompanion<Keys>(Keys.values()) {

        override val directoryName: String
            get() = "sessions"

    }

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
