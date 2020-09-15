package coverosR3z.authentication

import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.UserName
import coverosR3z.domainobjects.User

interface IAuthPersistence {
    fun createUser(name: UserName, hash: Hash, salt: String, employeeId: Int? = null)
    fun isUserRegistered(name : UserName) : Boolean
    fun getUser(name: UserName) : User?
 }