package coverosR3z.timerecording

import coverosR3z.misc.DEFAULT_PROJECT_NAME
import coverosR3z.misc.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.testLogger
import coverosR3z.server.types.*
import coverosR3z.timerecording.api.ProjectAPI
import coverosR3z.timerecording.types.maxProjectNameSizeMsg
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
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
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT_NAME.value))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.AUTHENTICATED, testLogger)

        assertEquals(StatusCode.SEE_OTHER, ProjectAPI.handlePost(sd).statusCode)
    }

    /**
     * Huge name
     */
    @Test
    fun testHandlePOSTNewProject_HugeName() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to "a".repeat(31)))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.AUTHENTICATED, testLogger)

        val ex = assertThrows(IllegalArgumentException::class.java){ ProjectAPI.handlePost(sd)}

        assertEquals(maxProjectNameSizeMsg, ex.message)
    }

    /**
     * Big name, but acceptable
     */
    @Test
    fun testHandlePOSTNewProject_BigName() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to "a".repeat(30)))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.AUTHENTICATED, testLogger)

        assertEquals(StatusCode.SEE_OTHER, ProjectAPI.handlePost(sd).statusCode)
    }

    /**
     * Missing data
     */
    @Test
    fun testHandlePOSTNewProject_noBody() {
        val data = PostBodyData()
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val ex = assertThrows(InexactInputsException::class.java){  ProjectAPI.handlePost(sd) }
        assertEquals("expected keys: [project_name]. received keys: []", ex.message)
    }
}