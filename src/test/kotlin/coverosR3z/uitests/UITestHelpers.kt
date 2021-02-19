package coverosR3z.uitests

import coverosR3z.FullSystem
import coverosR3z.authentication.api.LoginAPI
import coverosR3z.authentication.api.LogoutAPI
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.config.utility.SystemOptions
import coverosR3z.logging.LogTypes
import coverosR3z.logging.LoggingAPI
import coverosR3z.timerecording.api.CreateEmployeeAPI
import coverosR3z.timerecording.api.ProjectAPI
import coverosR3z.timerecording.api.SubmitTimeAPI
import coverosR3z.timerecording.api.ViewTimeAPI
import coverosR3z.webDriver
import org.junit.Assert.assertEquals
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver

enum class Drivers(val driver: () -> WebDriver){
    FIREFOX({ FirefoxDriver() }),
    CHROME({ ChromeDriver(ChromeOptions().setHeadless(false)) }),
    CHROME_HEADLESS({ ChromeDriver(ChromeOptions().setHeadless(true)) }),
}

/**
 * sets up everything needed to run the UI tests:
 * - starts the server on the port provided
 * - creates a new database for the server
 * - initializes the utilities needed
 * - creates a [PageObjectModel] so the UI tests have access to all this stuff,
 *   and so we can have a testing-oriented API
 *   @param port the port our server will run on, and thus the port our client should target
 */
fun startupTestForUI(domain: String = "http://localhost", port : Int, driver: () -> WebDriver = webDriver.driver) : PageObjectModelLocal {
    // start the server
    val fs = FullSystem.startSystem(SystemOptions(port = port, dbDirectory = null))

    return PageObjectModelLocal.make(driver(), port, fs.businessCode, fs, checkNotNull(fs.pmd), domain)
}

fun startupTestForUIWithoutServer(domain: String = "", port : Int, driver: () -> WebDriver = webDriver.driver) : PageObjectModel {
    return PageObjectModel.make(driver(), port, domain)
}

class LoginPage(private val driver: WebDriver, private val domain : String) {

    fun login(username: String, password: String) {
        driver.get("$domain/${LoginAPI.path}")
        driver.findElement(By.id(LoginAPI.Elements.USERNAME_INPUT.getId())).sendKeys(username)
        driver.findElement(By.id(LoginAPI.Elements.PASSWORD_INPUT.getId())).sendKeys(password)
        driver.findElement(By.id(LoginAPI.Elements.LOGIN_BUTTON.getId())).click()
    }
}

class RegisterPage(private val driver: WebDriver, private val domain : String) {

    fun register(username: String, password: String, employee: String) {
        driver.get("$domain/${RegisterAPI.path}")
        driver.findElement(By.id("username")).sendKeys(username)
        driver.findElement(By.id("password")).sendKeys(password)
        driver.findElement(By.id("employee")).findElement(By.xpath("//option[. = '$employee']")).click()
        driver.findElement(By.id("register_button")).click()
    }
}

class EnterEmployeePage(private val driver: WebDriver, private val domain : String) {

    fun enter(employee: String) {
        driver.get("$domain/${CreateEmployeeAPI.path}")
        driver.findElement(By.id(CreateEmployeeAPI.Elements.EMPLOYEE_INPUT.getId())).sendKeys(employee)
        driver.findElement(By.id(CreateEmployeeAPI.Elements.CREATE_BUTTON.getId())).click()
    }
}

class EnterProjectPage(private val driver: WebDriver, private val domain : String) {

    fun enter(project: String) {
        driver.get("$domain/${ProjectAPI.path}")
        driver.findElement(By.id(ProjectAPI.Elements.PROJECT_INPUT.getId())).sendKeys(project)
        driver.findElement(By.id(ProjectAPI.Elements.CREATE_BUTTON.getId())).click()
    }
}

class LoggingPage(private val driver: WebDriver, private val domain: String) {

    fun go() {
        driver.get("$domain/${LoggingAPI.path}")
    }

    fun setLoggingTrue(lt : LogTypes) {
        val id = when (lt) {
            LogTypes.AUDIT -> (LoggingAPI.Elements.AUDIT_INPUT.getId() + "true")
            LogTypes.WARN -> (LoggingAPI.Elements.WARN_INPUT.getId() + "true")
            LogTypes.DEBUG -> (LoggingAPI.Elements.DEBUG_INPUT.getId() + "true")
            LogTypes.TRACE -> (LoggingAPI.Elements.TRACE_INPUT.getId() + "true")
        }
        driver.findElement(By.id(id)).click()
    }

    fun setLoggingFalse(lt : LogTypes) {
        val id = when (lt) {
            LogTypes.AUDIT -> (LoggingAPI.Elements.AUDIT_INPUT.getId() + "false")
            LogTypes.WARN -> (LoggingAPI.Elements.WARN_INPUT.getId() + "false")
            LogTypes.DEBUG -> (LoggingAPI.Elements.DEBUG_INPUT.getId() + "false")
            LogTypes.TRACE -> (LoggingAPI.Elements.TRACE_INPUT.getId() + "false")
        }
        driver.findElement(By.id(id)).click()
    }

    fun save() {
        driver.findElement(By.id(LoggingAPI.Elements.SAVE_BUTTON.getId())).click()
    }

    fun isLoggingOn(lt : LogTypes) : Boolean {
        val id = when (lt) {
            LogTypes.AUDIT -> (LoggingAPI.Elements.AUDIT_INPUT.getId() + "true")
            LogTypes.WARN -> (LoggingAPI.Elements.WARN_INPUT.getId() + "true")
            LogTypes.DEBUG -> (LoggingAPI.Elements.DEBUG_INPUT.getId() + "true")
            LogTypes.TRACE -> (LoggingAPI.Elements.TRACE_INPUT.getId() + "true")
        }
        return driver.findElement(By.id(id)).getAttribute("checked") == "true"
    }

}

class LogoutPage(private val driver: WebDriver, private val domain: String) {

    fun go() {
        driver.get("$domain/${LogoutAPI.path}")
    }

}

class ViewTimePage(private val driver: WebDriver, private val domain: String) {

    fun getTimeForEntry(id: Int) : String {
        return driver.findElement(By.cssSelector("#time-entry-$id input[name=${ViewTimeAPI.Elements.TIME_INPUT.getElemName()}]")).getAttribute("value")
    }

    fun getDateForEntry(id: Int) : String {
        return driver.findElement(By.cssSelector("#time-entry-$id input[name=${ViewTimeAPI.Elements.DATE_INPUT.getElemName()}]")).getAttribute("value")
    }

    fun submitTimeForPeriod() {
        val submitButton = driver.findElement(By.cssSelector("#${ViewTimeAPI.Elements.SUBMIT_BUTTON.getId()}"))
        check(submitButton.text == "SUBMIT")
        return submitButton.click()
    }

    fun verifyPeriodIsSubmitted() : Boolean {
        val submitButton = driver.findElement(By.cssSelector("#${ViewTimeAPI.Elements.SUBMIT_BUTTON.getId()}"))
        return submitButton.text == "UNSUBMIT"
    }

    fun verifyPeriodIsUnsubmitted() : Boolean{
        val submitButton = driver.findElement(By.cssSelector("#${ViewTimeAPI.Elements.SUBMIT_BUTTON.getId()}"))
        return submitButton.text == "SUBMIT"
    }

    fun unsubmitForTimePeriod() {
        val submitButton = driver.findElement(By.cssSelector("#${ViewTimeAPI.Elements.SUBMIT_BUTTON.getId()}"))
        check(submitButton.text == "UNSUBMIT")
        return submitButton.click()
    }

    /**
     * Open the view time page for a particular time period.
     */
    fun gotoDate(date: String) {
        driver.get("$domain/${ViewTimeAPI.path}?date=$date")
    }

    fun goToPreviousPeriod() {
        driver.findElement(By.id(ViewTimeAPI.Elements.PREVIOUS_PERIOD.getId())).click()
    }

    fun goToNextPeriod() {
        driver.findElement(By.id(ViewTimeAPI.Elements.NEXT_PERIOD.getId())).click()
    }

    fun getCurrentPeriod() : String{
        val start = driver.findElement(By.name(SubmitTimeAPI.Elements.START_DATE.getElemName())).getAttribute("value")
        val end = driver.findElement(By.name(SubmitTimeAPI.Elements.END_DATE.getElemName())).getAttribute("value")
        return "$start - $end"
    }

    fun enterTime(project: String, time: String, details: String, date: String) {
        driver.get("$domain/${ViewTimeAPI.path}")
        val createTimeEntryRow = driver.findElement(By.id(ViewTimeAPI.Elements.CREATE_TIME_ENTRY_ROW.getId()))
        val projectSelector = createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.PROJECT_INPUT.getElemName()))
        projectSelector.findElement(By.xpath("//option[. = '$project']")).click()
        createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.TIME_INPUT.getElemName())).sendKeys(time)
        createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.DETAIL_INPUT.getElemName())).sendKeys(details)
        createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.DATE_INPUT.getElemName())).sendKeys(date)
        createTimeEntryRow.findElement(By.className(ViewTimeAPI.Elements.SAVE_BUTTON.getElemClass())).click()
        // we verify the time entry is registered later, so only need to test that we end up on the right page successfully
        assertEquals("your time entries", driver.title)
    }

    fun setProjectForNewEntry(project: String) {
        val createTimeEntryRow = driver.findElement(By.id(ViewTimeAPI.Elements.CREATE_TIME_ENTRY_ROW.getId()))
        val projectSelector = createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.PROJECT_INPUT.getElemName()))
        projectSelector.findElement(By.xpath("//option[. = '$project']")).click()
    }

    private fun setProjectForEditEntry(id: Int, project: String) {
        val projectSelector = driver
            .findElement(
                By.cssSelector("#time-entry-$id [name=${ViewTimeAPI.Elements.PROJECT_INPUT.getElemName()}]"))

        projectSelector.findElement(By.xpath("//option[. = '$project']")).click()
    }

    /**
     * simply clicks the CREATE button on the page
     */
    fun clickCreateNewTimeEntry() {
        driver
            .findElement(By.id(ViewTimeAPI.Elements.CREATE_TIME_ENTRY_ROW.getId()))
            .findElement(By.className(ViewTimeAPI.Elements.SAVE_BUTTON.getElemClass())).click()
    }

    /**
     * simply clicks the SAVE button on a particular edit row
     */
    fun clickSaveTimeEntry(id : Int) {
        driver
            .findElement(
                By.cssSelector("#time-entry-$id .${ViewTimeAPI.Elements.SAVE_BUTTON.getElemClass()}")).click()
    }

    /**
     * simply clicks the EDIT button on a particular read-only row
     */
    fun clickEditTimeEntry(id : Int) {
        driver
            .findElement(
                By.cssSelector("#time-entry-$id .${ViewTimeAPI.Elements.EDIT_BUTTON.getElemClass()}")).click()
    }

    /**
     * Understand: this is for creating a *new* time entry, not for editing
     * existing time entries
     */
    fun setTimeForNewEntry(time: String) {
        val timeInput = driver
            .findElement(By.id(ViewTimeAPI.Elements.CREATE_TIME_ENTRY_ROW.getId()))
            .findElement(By.cssSelector("input[name=${ViewTimeAPI.Elements.TIME_INPUT.getElemName()}]"))
        timeInput.clear()
        timeInput.sendKeys(time)
    }

    /**
     * Understand: this is for creating a *new* time entry, not for editing
     * existing time entries
     */
    fun setDateForNewEntry(date: String) {
        val dateInput = driver
            .findElement(By.id(ViewTimeAPI.Elements.CREATE_TIME_ENTRY_ROW.getId()))
            .findElement(By.cssSelector("input[name=${ViewTimeAPI.Elements.DATE_INPUT.getElemName()}]"))
        dateInput.clear()
        dateInput.sendKeys(date)
    }

    /**
     * Understand: this is for editing an existing time entry
     * @param time the time in hours to enter
     * @param id the id of the time entry, that's how we select the right row on the page
     */
    fun setTimeForEditingTimeEntry(id: Int, time: String) {
        val timeInput = driver.findElement(By.cssSelector("#time-entry-$id input[name=${ViewTimeAPI.Elements.TIME_INPUT.getElemName()}]"))
        timeInput.clear()
        timeInput.sendKeys(time)
    }

    private fun setDetailsForEditingTimeEntry(id: Int, details: String) {
        val detailInput = driver.findElement(By.cssSelector("#time-entry-$id input[name=${ViewTimeAPI.Elements.DETAIL_INPUT.getElemName()}]"))
        detailInput.clear()
        detailInput.sendKeys(details)
    }


    fun clearTheDateEntryOnEdit(id: Int) {
        val dateInput = driver.findElement(By.cssSelector("#time-entry-$id input[name=${ViewTimeAPI.Elements.DATE_INPUT.getElemName()}]"))
        dateInput.clear()
    }

    fun setTheDateEntryOnEdit(id: Int, dateString: String) {
        val dateInput = driver.findElement(By.cssSelector("#time-entry-$id input[name=${ViewTimeAPI.Elements.DATE_INPUT.getElemName()}]"))
        dateInput.clear()
        dateInput.sendKeys(dateString)
    }

    fun clearTheNewEntryDateEntry() {
        driver.findElement(By.id(ViewTimeAPI.Elements.CREATE_TIME_ENTRY_ROW.getId()))
            .findElement(By.cssSelector("input[name=${ViewTimeAPI.Elements.DATE_INPUT.getElemName()}]")).clear()
    }

    /**
     * @param id the id of the time entry we want to edit
     */
    fun editTime(id: Int, project: String, time: String, details: String, dateString: String) {
        clickEditTimeEntry(id)
        setProjectForEditEntry(id, project)
        setTimeForEditingTimeEntry(id, time)
        setDetailsForEditingTimeEntry(id, details)
        setTheDateEntryOnEdit(id, dateString)
        clickSaveTimeEntry(id)
    }


}
