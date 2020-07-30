package coverosR3z.domainobjects

import org.junit.Assert.assertTrue
import org.junit.Test

class UserTests {

    @Test
    fun `in the beginning, there was a user`() {
        val user = User(id=1, name="jenna", hash="abc123")
        assertTrue(user === user)
    }
}