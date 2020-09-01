package coverosR3z.authentication

import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.UserName

interface IAuthPersistence {
    fun createUser(name : UserName, hash : Hash, salt : String)
    fun isUserRegistered(name : UserName) : Boolean
 }