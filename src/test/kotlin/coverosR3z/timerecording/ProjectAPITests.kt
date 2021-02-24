package coverosR3z.timerecording

import coverosR3z.misc.DEFAULT_PROJECT_NAME
import coverosR3z.misc.DEFAULT_USER
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.fakeServerObjects
import coverosR3z.fakeTechempower
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.testLogger
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.*
import coverosR3z.timerecording.api.ProjectAPI
import coverosR3z.timerecording.types.maxProjectNameSizeMsg
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

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
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewProject() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT_NAME.value))
        val sd = makeServerData(data)

        assertEquals(StatusCode.SEE_OTHER, ProjectAPI.handlePost(sd).statusCode)
    }

    /**
     * Huge name
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewProject_HugeName() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to "a".repeat(31)))
        val sd = makeServerData(data)

        val ex = assertThrows(IllegalArgumentException::class.java){ ProjectAPI.handlePost(sd)}

        assertEquals(maxProjectNameSizeMsg, ex.message)
    }

    /**
     * Big name, but acceptable
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewProject_BigName() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to "a".repeat(30)))
        val sd = makeServerData(data)

        assertEquals(StatusCode.SEE_OTHER, ProjectAPI.handlePost(sd).statusCode)
    }

    /**
     * Missing data
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewProject_noBody() {
        val data = PostBodyData()
        val sd = makeServerData(data)
        val ex = assertThrows(InexactInputsException::class.java){  ProjectAPI.handlePost(sd) }
        assertEquals("expected keys: [project_name]. received keys: []", ex.message)
    }

    private fun makeServerData(data: PostBodyData): ServerData {
        val sd = ServerData(
            BusinessCode(tru, au, fakeTechempower),
            fakeServerObjects,
            AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.AUTHENTICATED, testLogger
        )
        return sd
    }
}