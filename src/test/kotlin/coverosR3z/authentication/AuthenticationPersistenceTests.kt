package coverosR3z.authentication

import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.Salt
import coverosR3z.domainobjects.UserName
import coverosR3z.persistence.PureMemoryDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationPersistenceTests {

    @Test
    fun `Should fail to find an unregistered user`() {
        val ap : IAuthPersistence = AuthenticationPersistence(PureMemoryDatabase())

        val result = ap.isUserRegistered(UserName("mitch"))

        assertEquals("we haven't registered anyone yet, so mitch shouldn't be registered", false, result)
    }

    @Test
    fun `Should be able to create a new user`() {
        val ap : IAuthPersistence = AuthenticationPersistence(PureMemoryDatabase())

        ap.createUser(UserName("jenna"), Hash.createHash(DEFAULT_PASSWORD), Salt(""), null)

        assertTrue(ap.isUserRegistered(UserName("jenna")))
    }

}