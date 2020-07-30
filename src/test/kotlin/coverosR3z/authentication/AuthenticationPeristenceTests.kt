package coverosR3z.authentication

import coverosR3z.persistence.PureMemoryDatabase
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticationPeristenceTests {

    @Test
    fun `should be possible to see if a executor is registered`() {
        val ap = AuthenticationPersistence(PureMemoryDatabase())

        val result = ap.isUserRegistered("mitch")

        assertEquals("we haven't registered anyone yet, so mitch shouldn't be registered", false, result)
    }
}