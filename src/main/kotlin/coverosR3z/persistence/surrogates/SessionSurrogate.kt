package coverosR3z.persistence.surrogates

import coverosR3z.domainobjects.DateTime
import coverosR3z.domainobjects.Session
import coverosR3z.domainobjects.User
import coverosR3z.exceptions.DatabaseCorruptedException
import coverosR3z.misc.checkParseToInt
import coverosR3z.misc.checkParseToLong
import coverosR3z.misc.decode
import coverosR3z.misc.encode

/**
 * A surrogate. See longer description for another surrogate at [TimeEntrySurrogate]
 */
data class SessionSurrogate(val sessionStr : String, val id: Int, val epochSecond: Long) {

    fun serialize(): String {
        return """{ s: ${encode(sessionStr)} , id: $id , e: $epochSecond }"""
    }

    companion object {

        fun deserialize(str: String): SessionSurrogate {
            try {
                val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap{it.groupValues}.toList()
                val sessionString = decode(groups[1])
                val id = checkParseToInt(groups[3])
                val epochSecond = checkParseToLong(groups[5])
                return SessionSurrogate(sessionString, id, epochSecond)
            } catch (ex : Throwable) {
                throw DatabaseCorruptedException("Unable to deserialize this text as session data: $str", ex)
            }
        }

        fun deserializeToSession(str: String, users: Set<User>) : Session {
            return fromSurrogate(deserialize(str), users)
        }

        fun toSurrogate(s : Session) : SessionSurrogate {
            return SessionSurrogate(s.sessionId, s.user.id.value, s.dt.epochSecond)
        }

        private fun fromSurrogate(ss: SessionSurrogate, users: Set<User>) : Session {
            val user = try {
                users.single { it.id.value == ss.id }
            } catch (ex : NoSuchElementException) {
                throw DatabaseCorruptedException("Unable to find a user with the id of ${ss.id}.  User set size: ${users.size}")
            }
            return Session(ss.sessionStr, user, DateTime(ss.epochSecond))
        }
    }
}