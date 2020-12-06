package coverosR3z.timerecording

import coverosR3z.DEFAULT_PROJECT_NAME
import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.domainobjects.maxProjectNameSizeMsg
import coverosR3z.domainobjects.projectNameNotNullMsg
import coverosR3z.exceptions.InexactInputsException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ProjectAPITests {


    lateinit var au : IAuthenticationUtilities
    lateinit var tru : ITimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * A basic happy path
     */
    @Test
    fun testHandlePOSTNewProject() {
        val data = mapOf(ProjectElements.PROJECT_INPUT.elemName to DEFAULT_PROJECT_NAME.value)
        handlePOSTCreatingProject(tru, DEFAULT_USER, data)
    }

    /**
     * Huge name
     */
    @Test
    fun testHandlePOSTNewProject_HugeName() {
        val data = mapOf(ProjectElements.PROJECT_INPUT.elemName to "a".repeat(31))
        val ex = assertThrows(IllegalArgumentException::class.java){handlePOSTCreatingProject(tru, DEFAULT_USER, data)}
        assertEquals(maxProjectNameSizeMsg, ex.message)
    }

    /**
     * Big name, but acceptable
     */
    @Test
    fun testHandlePOSTNewProject_BigName() {
        val data = mapOf(ProjectElements.PROJECT_INPUT.elemName to "a".repeat(30))
        handlePOSTCreatingProject(tru, DEFAULT_USER, data)
    }

    /**
     * Missing data
     */
    @Test
    fun testHandlePOSTNewProject_noBody() {
        val data = emptyMap<String,String>()
        val ex = assertThrows(InexactInputsException::class.java){handlePOSTCreatingProject(tru, DEFAULT_USER, data)}
        assertEquals("expected keys: [project_name]. received keys: []", ex.message)
    }
}