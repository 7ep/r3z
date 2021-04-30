package coverosR3z.uitests

import coverosR3z.authentication.api.LoginAPI
import coverosR3z.authentication.api.LogoutAPI
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.system.config.utility.SystemOptions
import coverosR3z.system.logging.LogTypes
import coverosR3z.system.logging.LoggingAPI
import coverosR3z.system.misc.testLogger
import coverosR3z.system.misc.types.Date
import coverosR3z.system.utility.FullSystem
import coverosR3z.system.utility.FullSystem.Companion.initializeBusinessCode
import coverosR3z.timerecording.api.CreateEmployeeAPI
import coverosR3z.timerecording.api.ProjectAPI
import coverosR3z.timerecording.api.ViewTimeAPI
import coverosR3z.webDriver
import org.junit.Assert.assertEquals
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver

enum class Drivers(val driver: () -> WebDriver){
    FIREFOX(
        { FirefoxDriver(
            org.openqa.selenium.firefox.FirefoxOptions()
                .setAcceptInsecureCerts(true)
                .addArguments("--width=800", "--height=500")
        ) }
    ),

    CHROME(
        { ChromeDriver(
            ChromeOptions()
                .addArguments("--window-size=800,500")
                .setHeadless(false)
                .setAcceptInsecureCerts(true)) }
    ),

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
fun startupTestForUI(
    domain: String = "localhost",
    port: Int,
    driver: () -> WebDriver = webDriver.driver,
    directory: String? = null,
    allLoggingOn: Boolean = false,
    allLoggingOff: Boolean = false,
): PageObjectModelLocal {
    // start the server
    val fs = FullSystem.startSystem(SystemOptions(port = port, sslPort = port + 443, dbDirectory = directory, allLoggingOn = allLoggingOn, allLoggingOff = allLoggingOff))

    val bc = initializeBusinessCode(fs.pmd, testLogger)

    return PageObjectModelLocal.make(driver(), port, port + 443, bc, fs, checkNotNull(fs.pmd), domain)
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

    fun register(username: String, invitationCode: String) {
        driver.get("$domain/${RegisterAPI.path}?${RegisterAPI.Elements.INVITATION_INPUT.getElemName()}=$invitationCode")
        driver.findElement(By.id("username")).sendKeys(username)
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

    /**
     * Gets the number of hours for a readonly line.
     */
    fun getTimeForEntry(id: Int) : String {
        return driver.findElement(By.cssSelector("#time-entry-$id > :nth-child(2)")).text
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
    fun gotoDate(date: Date) {
        driver.get("$domain/${ViewTimeAPI.path}?date=${date.stringValue}")
    }

    fun goToPreviousPeriod() {
        driver.findElement(By.id(ViewTimeAPI.Elements.PREVIOUS_PERIOD.getId())).click()
    }

    fun getCurrentPeriod() : String{
        val start = driver.findElement(By.id("timeperiod_display_start")).text
        val end = driver.findElement(By.id("timeperiod_display_end")).text
        return "$start - $end"
    }

    /**
     * Enters a new time entry.
     */
    fun enterTime(project: String, time: String, details: String, date: Date) {
        gotoDate(date)
        val createTimeEntryRow = driver.findElement(By.id(ViewTimeAPI.Elements.CREATE_TIME_ENTRY_FORM.getId()))
        val projectSelector = createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.PROJECT_INPUT.getElemName()))
        projectSelector.clear()
        projectSelector.sendKeys(project)
        createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.TIME_INPUT.getElemName())).sendKeys(time)
        createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.DETAIL_INPUT.getElemName())).sendKeys(details)
        setDateForNewEntry(date)
        clickCreateNewTimeEntry()
        // we verify the time entry is registered later, so only need to test that we end up on the right page successfully
        assertEquals("Your time entries", driver.title)
    }

    fun setProjectForNewEntry(project: String) {
        val projectSelector = driver.findElement(By.id(ViewTimeAPI.Elements.PROJECT_INPUT_CREATE.getId()))
        projectSelector.clear()
        projectSelector.sendKeys(project)
    }

    private fun setProjectForEditEntry(project: String) {
        val projectSelector = driver.findElement(By.id(ViewTimeAPI.Elements.PROJECT_INPUT_EDIT.getElemName()))
        projectSelector.clear()
        projectSelector.sendKeys(project)
    }

    /**
     * simply clicks the CREATE button on the page
     */
    fun clickCreateNewTimeEntry() {
        driver.findElement(By.id(ViewTimeAPI.Elements.CREATE_BUTTON.getId())).click()
    }

    /**
     * simply clicks the SAVE button on a particular edit row
     */
    fun clickSaveTimeEntry() {
        driver.findElement(By.id(ViewTimeAPI.Elements.SAVE_BUTTON.getId())).click()
    }

    /**
     * simply clicks the EDIT button on a particular read-only row
     */
    fun clickEditTimeEntry(id : Int) {
        driver
            .findElement(
                By.cssSelector("#time-entry-$id > :nth-child(4)")).click()
    }

    /**
     * Understand: this is for creating a *new* time entry, not for editing
     * existing time entries
     */
    fun setTimeForNewEntry(time: String) {
        val timeInput = driver.findElement(By.id(ViewTimeAPI.Elements.TIME_INPUT_CREATE.getElemName()))
        timeInput.clear()
        timeInput.sendKeys(time)
    }

    /**
     * Understand: this is for creating a *new* time entry, not for editing
     * existing time entries
     */
    fun setDateForNewEntry(date: Date) {
        (driver as JavascriptExecutor)
            .executeScript("document.getElementById('${ViewTimeAPI.Elements.DATE_INPUT_CREATE.getElemName()}').setAttribute('value','${date.stringValue}');")
    }

    /**
     * Understand: this is for creating a *new* time entry, not for editing
     * existing time entries
     * See [setDateForNewEntry] for the equivalent that uses a strongly-typed date
     * object.  This one takes a string, which lets us use values the [Date] won't.
     * Note: Format of the string should be YYYY-MM-DD
     */
    fun setDateForNewEntryString(date: String) {
        (driver as JavascriptExecutor)
            .executeScript("document.getElementById('${ViewTimeAPI.Elements.DATE_INPUT_CREATE.getElemName()}').setAttribute('value','$date');")
    }

    /**
     * Understand: this is for editing an existing time entry
     * @param time the time in hours to enter
     */
    fun setTimeForEditingTimeEntry(time: String) {
        val timeInput = driver.findElement(By.id(ViewTimeAPI.Elements.TIME_INPUT_EDIT.getId()))
        timeInput.clear()
        timeInput.sendKeys(time)
    }

    /**
     * Allows setting text into the details input field
     */
    private fun setDetailsForEditingTimeEntry(details: String) {
        val detailInput = driver.findElement(By.id(ViewTimeAPI.Elements.DETAILS_INPUT_EDIT.getId()))
        detailInput.clear()
        detailInput.sendKeys(details)
    }


    /**
     * Resets the date entry when editing.
     */
    fun clearTheDateEntryOnEdit() {
        val dateInput =  driver.findElement(By.id(ViewTimeAPI.Elements.DATE_INPUT_EDIT.getId()))
        dateInput.clear()
    }

    /**
     * Sets the date field when editing
     */
    fun setTheDateEntryOnEdit(date: Date) {
        (driver as JavascriptExecutor).executeScript("document.getElementById('${ViewTimeAPI.Elements.DATE_INPUT_EDIT.getId()}').setAttribute('value','${date.stringValue}');")
    }

    /**
     * Understand: this is for creating a *new* time entry, not for editing
     * existing time entries
     * See [setTheDateEntryOnEdit] for the equivalent that uses a strongly-typed date
     * object.  This one takes a string, which lets us use values the [Date] won't.
     * Note: Format of the string should be YYYY-MM-DD
     */
    fun setTheDateEntryOnEditString(date: String) {
        (driver as JavascriptExecutor).executeScript("document.getElementById('${ViewTimeAPI.Elements.DATE_INPUT_EDIT.getId()}').setAttribute('value','$date');")
    }

    fun clearTheNewEntryDateEntry() {
        driver.findElement(By.id(ViewTimeAPI.Elements.DATE_INPUT_CREATE.getElemName())).clear()
    }

    /**
     * @param id the id of the time entry we want to edit
     */
    fun editTime(id: Int, project: String, time: String, details: String, date: Date) {
        clickEditTimeEntry(id)
        setProjectForEditEntry(project)
        setTimeForEditingTimeEntry(time)
        setDetailsForEditingTimeEntry(details)
        setTheDateEntryOnEdit(date)
        clickSaveTimeEntry()
    }


}