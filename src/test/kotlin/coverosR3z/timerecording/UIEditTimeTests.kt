package coverosR3z.timerecording

import coverosR3z.bddframework.BDD
import coverosR3z.misc.DEFAULT_DATE_STRING
import coverosR3z.misc.DEFAULT_PASSWORD
import coverosR3z.timerecording.api.ViewTimeAPI
import coverosR3z.uitests.PageObjectModelLocal
import coverosR3z.uitests.UITest
import coverosR3z.uitests.startupTestForUI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver

class UIEditTimeTests {

    @BDD
    @UITest
    @Test
    fun `editTime - An employee should be able to edit the number of hours worked from a previous time entry` () {
        val s = EditTimeUserStory.getScenario("editTime - An employee should be able to edit the number of hours worked from a previous time entry")

        loginAsUserAndCreateProject("Andrea", "projectb")
        s.markDone("Given Andrea has a previous time entry with 1 hour,")

        enterInitialTime()
        s.markDone("when she changes the entry to two hours,")

        changeTime()
        s.markDone("then the system indicates the two hours was persisted")

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


    companion object {
        private const val port = 4001
        private lateinit var pom : PageObjectModelLocal

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
        pom = startupTestForUI(port = port)
    }

    @After
    fun cleanup() {
        pom.server.halfOpenServerSocket.close()
        pom.driver.quit()
    }

    private fun logout() {
        pom.lop.go()
    }


    private fun enterInitialTime() {
        // when the employee enters their time
        enterTimeForEmployee("projectb")
        pom.driver.get("${pom.domain}/${ViewTimeAPI.path}")
    }

    private fun changeTime() {
        // put the row into edit mode
        pom.driver.findElement(By.cssSelector("#time-entry-1 .${ViewTimeAPI.Elements.EDIT_BUTTON.getElemClass()}")).click()

        val timeField =
            pom.driver.findElement(By.cssSelector("#time-entry-1 input[name=${ViewTimeAPI.Elements.TIME_INPUT.getElemName()}]"))
        timeField.clear()
        timeField.sendKeys("2")

        // save the new time
        pom.driver.findElement(By.cssSelector("#time-entry-1 .${ViewTimeAPI.Elements.SAVE_BUTTON.getElemClass()}")).click()

        // confirm the change
        assertEquals("2.00", pom.vtp.getTimeForEntry(1))
    }

    private fun enterTimeForEmployee(project: String) {
        val dateString = if (pom.driver is ChromeDriver) {
            "06122020"
        } else {
            DEFAULT_DATE_STRING
        }

        // Enter time
        pom.etp.enterTime(project, "1", "", dateString)
    }

    private fun loginAsUserAndCreateProject(user: String, project: String) {
        val password = DEFAULT_PASSWORD.value

        // register and login
        pom.rp.register(user, password, "Administrator")
        pom.lp.login(user, password)

        // Create project
        pom.epp.enter(project)
    }

}