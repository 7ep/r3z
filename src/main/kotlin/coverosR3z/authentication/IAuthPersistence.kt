package coverosR3z.authentication

interface IAuthPersistence {
    fun createExecutor(name : String)
    fun isUserRegistered(name : String) : Boolean
 }