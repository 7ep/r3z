package coverosR3z.authentication

import coverosR3z.authentication.types.Roles
import coverosR3z.misc.*
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.uitests.PageObjectModelLocal
import coverosR3z.uitests.UITestCategory
import coverosR3z.uitests.startupTestForUI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.experimental.categories.Category
import java.io.File

class UIRoleTests {

    private val adminUsername = "adrian"

    @Category(UITestCategory::class)
    @Test
    fun verifyRolesCanSeeWhatTheyShould() {
        TODO("Implement")
        `setup some default projects and employees`()
        loginAdmin()
        //verify admin sees EVERYTHING
        //create employee
        //login as employee
        //verify the employee only sees what they should see
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
        private const val port = 4003
        private lateinit var pom: PageObjectModelLocal
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
        val databaseDirectorySuffix = "uiroletests_on_port_$port"
        databaseDirectory = "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/"
        File(databaseDirectory).deleteRecursively()
        pom = startupTestForUI(port = port, directory = databaseDirectory)
    }

    @After
    fun finish() {
        pom.fs.shutdown()
        val pmd = DatabaseDiskPersistence(databaseDirectory, testLogger).startWithDiskPersistence()
        Assert.assertEquals(pom.pmd, pmd)
        pom.driver.quit()
    }

    private fun logout() {
        pom.lop.go()
    }


    private fun loginAdmin() {
        logout()

//        pom.rp.register(adminUsername, DEFAULT_PASSWORD.value, adminEmployee)
        pom.lp.login(adminUsername, DEFAULT_PASSWORD.value)
    }

    private fun `setup some default projects and employees`() {
        logout()
        // register and login the Admin
        pom.rp.register(adminUsername, DEFAULT_PASSWORD.value, DEFAULT_ADMINISTRATOR_NAME.value)
        val user = pom.pmd.UserDataAccess().read { users -> users.single{ it.name.value == adminUsername }}
        pom.businessCode.au.addRoleToUser(user, Roles.ADMIN)
        pom.lp.login(adminUsername, DEFAULT_PASSWORD.value)

        // Create a default project
        pom.epp.enter(DEFAULT_PROJECT.toString())
    }
}