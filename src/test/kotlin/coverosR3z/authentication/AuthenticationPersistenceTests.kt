package coverosR3z.authentication

import coverosR3z.persistence.PureMemoryDatabase
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticationPersistenceTests {

    @Test
    fun `Should fail to find an unregistered user`() {
        val ap : IAuthPersistence = AuthenticationPersistence(PureMemoryDatabase())

        val result = ap.isUserRegistered("mitch")

        assertEquals("we haven't registered anyone yet, so mitch shouldn't be registered", false, result)
    }

    // Should be able to register a user and confirm their registration

}