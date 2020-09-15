package coverosR3z.authentication

import coverosR3z.domainobjects.Employee
import coverosR3z.persistence.PureMemoryDatabase
import org.junit.Test
import coverosR3z.createTimeEntryPreDatabase
import org.junit.Assert.assertEquals

class UserManagementTests {
    /**
     * When a user is registered, they must supply an employee identifier provided by some
     * administrator. We cannot allow a user to simply create their corresponding employee--
     * they must be invited into the org, with some prior setup.
     */
    @Test
    fun `Registering a user should require a employee identifier`() {
        val cua = CurrentUserAccessor() // since we have a method to clear this, we can share it between tests
        cua.clearCurrentUserTestOnly()
        val pmd = PureMemoryDatabase()
        val authPersistence = AuthenticationPersistence(pmd)
        val au = AuthenticationUtilities(authPersistence)
        au.register("matt", "password1234", 17)// let's assume an admin gave us '17' as our employeeId
        val loginResult = au.login("matt", "password1234")
        println(loginResult.user)
        val user = cua.get()
        println(user)
        val entry = createTimeEntryPreDatabase(employee=Employee(17, "matt"))
        assertEquals(user?.employeeId, entry.employee.id)
    }

}