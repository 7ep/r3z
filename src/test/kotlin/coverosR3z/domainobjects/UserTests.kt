package coverosR3z.domainobjects

import coverosR3z.getResourceAsText
import org.junit.Assert.assertEquals
import org.junit.Test

class UserTests {

    private val text = getResourceAsText("/coverosR3z/domainobjects/user_serialized1.txt")
    private val user = User(1, "some user")

    @Test
    fun `can serialize User`() {
        assertEquals(text, user.serialize())
    }

    @Test
    fun `can deserialize User`() {
        assertEquals(user, User.deserialize(text))
    }
}