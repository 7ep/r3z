package coverosR3z.uitests

import coverosR3z.bddframework.BDD
import coverosR3z.timerecording.CreateEmployeeUserStory
import coverosR3z.timerecording.api.ViewEmployeesAPI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class UICreateEmployee {

    @BDD
    @UITest
    @Test
    fun `createEmployee - I should be able to create an employee`() {
        val s = CreateEmployeeUserStory.getScenario("createEmployee - I should be able to create an employee")

        s.markDone("Given the company has hired a new employee, Andrea,")

        addAndreaEmployee()
        s.markDone("when I add her as an employee,")

        confirmSuccess()
        s.markDone("then the system indicates success.")

        logout()
    }


    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    private fun confirmSuccess() {
        assertEquals("SUCCESS", pom.driver.title)
        pom.driver.get("${pom.domain}/${ViewEmployeesAPI.path}")
    }

    private fun addAndreaEmployee() {
        pom.rp.register("employeemaker", "password12345", "Administrator")
        pom.lp.login("employeemaker", "password12345")
        pom.eep.enter("a new employee")
    }


    companion object {
        private const val port = 4000
        private lateinit var pom : PageObjectModel

        @BeforeClass
        @JvmStatic
        fun setup() {
            // install the most-recent chromedriver
            WebDriverManager.chromedriver().setup()
            WebDriverManager.firefoxdriver().setup()
        }

    }

    @Before
    fun init() {
        pom = startupTestForUI(port)
    }

    @After
    fun cleanup() {
        pom.server.halfOpenServerSocket.close()
        pom.driver.quit()
    }

    private fun logout() {
        pom.lop.go()
    }

}