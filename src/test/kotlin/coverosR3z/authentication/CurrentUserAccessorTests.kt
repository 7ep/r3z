package coverosR3z.authentication

import coverosR3z.DEFAULT_USER
import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.User
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test

class CurrentUserAccessorTests {

    val NOT_DEFAULT_USER = User(123, "Mitch", Hash.createHash(""), "", null)

    @Test
    fun `we should get null if we try to get the user when the user has not been set`() {
        val cua = CurrentUserAccessor()
        cua.clearCurrentUserTestOnly()
        val user = cua.get()
        assertNull(user)
    }

    @Test
    fun `happy path - should successfully set and get the user`() {
        val cua = CurrentUserAccessor()
        cua.clearCurrentUserTestOnly()
        cua.set(DEFAULT_USER)
        assertEquals(DEFAULT_USER, cua.get())
    }

    @Test
    fun `test should throw exception if user is already set`() {
        val cua = CurrentUserAccessor()
        cua.clearCurrentUserTestOnly()
        cua.set(DEFAULT_USER)
        assertThrows(AssertionError::class.java) {cua.set(NOT_DEFAULT_USER)}
    }

    @Test
    fun `test that two different instances of a class access the same singleton object`() {
        val cua = CurrentUserAccessor()
        cua.clearCurrentUserTestOnly()
        cua.set(DEFAULT_USER)
        val cua2 = CurrentUserAccessor()
        assertEquals(DEFAULT_USER, cua2.get())
    }

    /**
     * This is to show that if we don't intentionally clear the value
     * of the CurrentUser object (a singleton), it will carry over to
     * other tests.  You will need to run clearCurrentUserTestOnly in
     * order to get it cleared for a test.
     */
    @Test
    @Ignore("This was just to check that previous tests will impact this test.  This is here for historical purposes")
    fun `test that setting a value in a previous test will carry over to this test`() {
        val cua = CurrentUserAccessor()
        assertEquals(DEFAULT_USER, cua.get())
    }

}