package coverosR3z.domainobjects

import coverosR3z.getResourceAsText
import org.junit.Assert.assertEquals
import org.junit.Test

class UserTests {

    val text = getResourceAsText("/coverosR3z/domainobjects/user_serialized1.txt")

    @Test
    fun `can serialize User`() {
        val user = User(1, "some user")
        assertEquals(text, user.serialize())
    }

    @Test
    fun `can deserialize User`() {
        val user = User(1, "some user")
        assertEquals(user, User.deserialize(text))
    }
}