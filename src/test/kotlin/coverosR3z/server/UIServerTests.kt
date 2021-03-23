package coverosR3z.server

import coverosR3z.authentication.api.ChangePasswordAPI
import coverosR3z.authentication.api.LoginAPI
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.types.*
import coverosR3z.logging.LogTypes
import coverosR3z.misc.DEFAULT_DB_DIRECTORY
import coverosR3z.misc.DEFAULT_PASSWORD
import coverosR3z.misc.testLogger
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.server.api.HomepageAPI
import coverosR3z.uitests.PageObjectModelLocal
import coverosR3z.uitests.UITestCategory
import coverosR3z.uitests.startupTestForUI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category
import org.openqa.selenium.By
import java.io.File

class UIServerTests {

    @Category(UITestCategory::class)
    @Test
    fun serverTests() {
        `Go to an unknown page, expecting a not-found error`()
        `Go to an unknown page on an insecure port, expecting a not-found error`()
        `Try logging in with invalid credentials, expecting to be forbidden`()
        `Try hitting the insecure login page, expecting to be redirected to the secure one`()
        `head to the homepage on an insecure endpoint and find ourselves redirected to the SSL endpoint`()
        `general - should be able to see the homepage and the authenticated homepage`()
        `general - I should be able to change the logging settings`()
        `validation - Validation should stop me entering invalid input on the registration page`()
        `change admin password, relogin, create new employee, use invitation and change their password and login`()
    }

    private fun `change admin password, relogin, create new employee, use invitation and change their password and login`() {
        pom.lp.login("employeemaker", "password12345")
        // go to the change password page
        pom.driver.get("${pom.sslDomain}/${ChangePasswordAPI.path}")
        // confirm the title
        assertEquals("change password", pom.driver.title)
        // disagree to change the password
        pom.driver.findElement(By.id(ChangePasswordAPI.Elements.BRING_ME_BACK.getId())).click()
        // confirm we are back on the homepage
        assertEquals("Authenticated Homepage", pom.driver.title)
        // go back to the change password page
        pom.driver.get("${pom.sslDomain}/${ChangePasswordAPI.path}")
        // agree this time to change the password
        pom.driver.findElement(By.id(ChangePasswordAPI.Elements.YES_BUTTON.getId())).click()
        // confirm the title
        assertEquals("New password generated", pom.driver.title)
        val newPassword = pom.driver.findElement(By.id(ChangePasswordAPI.Elements.NEW_PASSWORD.getId())).text
        logout()
        // confirm the new password works
        pom.lp.login("employeemaker", newPassword)
        assertEquals("Authenticated Homepage", pom.driver.title)
        // create a new employee
        pom.eep.enter("hank")
        val invitationUrl = pom.driver.findElement(By.xpath("//*[text() = 'hank']/..//a")).getAttribute("href")
        logout()
        pom.driver.get(invitationUrl)
        pom.driver.findElement(By.id("username")).sendKeys("hank")
        pom.driver.findElement(By.id("register_button")).click()
        val hankRegistrationPassword = pom.driver.findElement(By.id(RegisterAPI.Elements.NEW_PASSWORD.getId())).text
        pom.lp.login("hank", hankRegistrationPassword)
        // go to the change password page
        pom.driver.get("${pom.sslDomain}/${ChangePasswordAPI.path}")
        // agree to change the password
        pom.driver.findElement(By.id(ChangePasswordAPI.Elements.YES_BUTTON.getId())).click()
        // confirm the title
        assertEquals("New password generated", pom.driver.title)
        val hankNewPassword = pom.driver.findElement(By.id(ChangePasswordAPI.Elements.NEW_PASSWORD.getId())).text
        logout()
        // confirm the new password works
        pom.lp.login("hank", hankNewPassword)
        assertEquals("your time entries", pom.driver.title)
        logout()
    }

    private fun `head to the homepage on an insecure endpoint and find ourselves redirected to the SSL endpoint`() {
        pom.driver.get("${pom.insecureDomain}/${HomepageAPI.path}")
        assertEquals("login page", pom.driver.title)
    }

    private fun `general - should be able to see the homepage and the authenticated homepage`() {
        pom.driver.get("${pom.sslDomain}/${HomepageAPI.path}")
        assertEquals("login page", pom.driver.title)
        val adminEmployee = pom.businessCode.tru.listAllEmployees().single{it.name.value == "Administrator"}
        val (_, user) = pom.businessCode.au.registerWithEmployee(UserName("employeemaker"), Password("password12345"), adminEmployee)
        pom.businessCode.au.addRoleToUser(user, Role.ADMIN)
        pom.lp.login("employeemaker", "password12345")
        pom.driver.get("${pom.sslDomain}/${HomepageAPI.path}")
        assertEquals("Authenticated Homepage", pom.driver.title)
        logout()
    }

    private fun `general - I should be able to change the logging settings`() {
        // Given I am an admin
        pom.lp.login("employeemaker", "password12345")
        pom.llp.go()
        // When I set Warn-level logging to not log
        pom.llp.setLoggingFalse(LogTypes.WARN)
        pom.llp.save()
        pom.llp.go()
        // Then that logging is set to not log
        assertFalse(pom.llp.isLoggingOn(LogTypes.WARN))
        logout()
    }

    private fun `validation - Validation should stop me entering invalid input on the registration page`() {
        pom.lp.login("employeemaker", "password12345")
        pom.eep.enter("alice")
        val invitationCode = pom.businessCode.au.listAllInvitations().single {it.employee.name.value == "alice"}

        logout()

        // validation won't allow it through - missing username
        disallowBecauseMissingUsername(invitationCode)

        // validation won't allow it through - username too short
        tooShortUsername(invitationCode)

        // Text entry will stop taking characters at the maximum size, so
        // what gets entered will just be truncated to [maxUserNameSize]
        tooLongerUsername(invitationCode)
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
        private lateinit var databaseDirectory : String

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
        val databaseDirectorySuffix = "uiservertests_on_port_$port"
        databaseDirectory = "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/"
        File(databaseDirectory).deleteRecursively()
        pom = startupTestForUI(port = port, directory = databaseDirectory)
    }

    @After
    fun finish() {
        pom.fs.shutdown()
        val pmd = DatabaseDiskPersistence(databaseDirectory, testLogger).startWithDiskPersistence()
        assertEquals(pom.pmd, pmd)
        pom.driver.quit()
    }

    private fun `Try logging in with invalid credentials, expecting to be forbidden`() {
        pom.lp.login("userabc", DEFAULT_PASSWORD.value)
        assertEquals("401 error", pom.driver.title)
    }

    private fun `Try hitting the insecure login page, expecting to be redirected to the secure one`() {
        pom.driver.get("${pom.insecureDomain}/${LoginAPI.path}")
        assertEquals("login page", pom.driver.title)
    }

    private fun `Go to an unknown page, expecting a not-found error`() {
        pom.driver.get("${pom.sslDomain}/does-not-exist")
        assertEquals("404 error", pom.driver.title)
    }

    private fun `Go to an unknown page on an insecure port, expecting a not-found error`() {
        pom.driver.get("${pom.insecureDomain}/does-not-exist")
        assertEquals("404 error", pom.driver.title)
    }

    private fun logout() {
        pom.lop.go()
    }

    private fun tooLongerUsername(invitation: Invitation) {
        val tooLongUsername = "a".repeat(maxUserNameSize + 1)
        pom.rp.register(tooLongUsername, invitation.code.value)
        assertFalse(pom.pmd.dataAccess<User>(User.directoryName).read { users -> users.any { it.name.value == tooLongUsername } })
    }

    private fun tooShortUsername(invitation: Invitation) {
        pom.rp.register("a".repeat(minUserNameSize - 1), invitation.code.value)
        assertEquals("register", pom.driver.title)
    }

    private fun disallowBecauseMissingUsername(invitation: Invitation) {
        pom.rp.register("", invitation.code.value)
        assertEquals("register", pom.driver.title)
    }

}