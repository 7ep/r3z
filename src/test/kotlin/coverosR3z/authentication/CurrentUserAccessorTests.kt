package coverosR3z.authentication

import coverosR3z.DEFAULT_USER
import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CurrentUserAccessorTests {

    val NOT_DEFAULT_USER = User(123, "Mitch", Hash.createHash(""), "", null)

    @Test
    fun `we should get an exception if we try to get the user when the user has not been set`() {
        val cua = CurrentUserAccessor()
        assertThrows(AssertionError::class.java){cua.get()}
    }

    @Test
    fun `happy path - should successfully set the user`() {
        val cua = CurrentUserAccessor()
        cua.set(DEFAULT_USER)
        assertEquals(DEFAULT_USER, cua.get())
    }

    @Test
    fun `test should throw exception if user is already set`() {
        val cua = CurrentUserAccessor()
        cua.set(DEFAULT_USER)
        assertThrows(AssertionError::class.java) {cua.set(NOT_DEFAULT_USER)}
    }

    @Test
    fun foo() {
        val cua = CurrentUserAccessor()
        cua.set(DEFAULT_USER)
        val cua2 = CurrentUserAccessor()
        assertThrows(AssertionError::class.java) {cua2.set(NOT_DEFAULT_USER)}

    }

}