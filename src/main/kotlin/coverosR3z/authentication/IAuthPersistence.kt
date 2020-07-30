package coverosR3z.authentication

interface IAuthPersistence {
    fun createUser(name : String)
    fun isUserRegistered(name : String) : Boolean
 }