package coverosR3z.persistence.surrogates

import coverosR3z.domainobjects.*
import coverosR3z.exceptions.DatabaseCorruptedException
import coverosR3z.misc.checkParseToInt
import coverosR3z.misc.decode
import coverosR3z.misc.encode

/**
 * A surrogate. See longer description for another surrogate at [TimeEntrySurrogate]
 */
data class UserSurrogate(val id: Int, val name: String, val hash: String, val salt: String, val empId: Int?) {

    fun serialize(): String {
        return """{ id: $id , name: ${encode(name)} , hash: ${encode(hash)} , salt: ${encode(salt)} , empId: ${empId ?: "null"} }"""
    }

    companion object {

        fun deserialize(str : String) : UserSurrogate {
            try {
                val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap{it.groupValues}.toList()
                val id = checkParseToInt(groups[1])
                val empId: Int? = if (groups[9] == "null") null else checkParseToInt(groups[9])
                return UserSurrogate(id, decode(groups[3]), decode(groups[5]), decode(groups[7]), empId)
            } catch (ex : Throwable) {
                throw DatabaseCorruptedException("Unable to deserialize this text as user data: $str", ex)
            }
        }

        fun deserializeToUser(str : String) : User {
            return fromSurrogate(deserialize(str))
        }

        fun toSurrogate(u : User) : UserSurrogate {
            return UserSurrogate(u.id.value, u.name.value, u.hash.value, u.salt.value, u.employeeId?.value)
        }

        private fun fromSurrogate(us: UserSurrogate) : User {
            val empId : EmployeeId? = if (us.empId == null) {
                null
            } else {
                EmployeeId(us.empId)
            }
            return User(UserId(us.id), UserName(us.name), Hash(us.hash), Salt(us.salt), empId)
        }

    }
}