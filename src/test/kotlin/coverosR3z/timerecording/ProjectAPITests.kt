package coverosR3z.timerecording

import coverosR3z.DEFAULT_PROJECT_NAME
import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.timerecording.types.maxProjectNameSizeMsg
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.server.types.AnalyzedHttpData
import coverosR3z.server.types.AuthStatus
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.server.utility.doPOSTAuthenticated
import coverosR3z.timerecording.api.ProjectAPI
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
        val data = mapOf(ProjectAPI.Elements.PROJECT_INPUT.elemName to DEFAULT_PROJECT_NAME.value)
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.AUTHENTICATED)

        assertEquals(StatusCode.OK, ProjectAPI.handlePost(sd).statusCode)
    }

    /**
     * Huge name
     */
    @Test
    fun testHandlePOSTNewProject_HugeName() {
        val data = mapOf(ProjectAPI.Elements.PROJECT_INPUT.elemName to "a".repeat(31))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.AUTHENTICATED)

        val ex = assertThrows(IllegalArgumentException::class.java){ ProjectAPI.handlePost(sd)}

        assertEquals(maxProjectNameSizeMsg, ex.message)
    }

    /**
     * Big name, but acceptable
     */
    @Test
    fun testHandlePOSTNewProject_BigName() {
        val data = mapOf(ProjectAPI.Elements.PROJECT_INPUT.elemName to "a".repeat(30))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.AUTHENTICATED)

        assertEquals(StatusCode.OK, ProjectAPI.handlePost(sd).statusCode)
    }

    /**
     * Missing data
     */
    @Test
    fun testHandlePOSTNewProject_noBody() {
        val data = emptyMap<String,String>()
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.AUTHENTICATED)
        val ex = assertThrows(InexactInputsException::class.java){  ProjectAPI.handlePost(sd) }
        assertEquals("expected keys: [project_name]. received keys: []", ex.message)
    }
}