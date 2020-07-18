package coverosR3z.domainobjects

import coverosR3z.getResourceAsText
import org.junit.Assert
import org.junit.Test

class ProjectTests {

    private val text = getResourceAsText("/coverosR3z/domainobjects/project_serialized1.txt")
    private val project = Project(1, "some project")

    @Test
    fun `can serialize Project`() {
        Assert.assertEquals(text, project.serialize())
    }

    @Test
    fun `can deserialize Project`() {
        Assert.assertEquals(project, Project.deserialize(text))
    }
}