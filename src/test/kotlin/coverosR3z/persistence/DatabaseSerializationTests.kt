package coverosR3z.persistence

import coverosR3z.misc.DEFAULT_PROJECT
import coverosR3z.persistence.utility.DatabaseSerialization
import coverosR3z.persistence.utility.DatabaseSerialization.Companion.databaseFileSuffix
import coverosR3z.timerecording.types.Project
import coverosR3z.timerecording.types.ProjectId
import coverosR3z.timerecording.types.ProjectName
import org.junit.Test
import java.io.File
import org.junit.Assert.*

class DatabaseSerializationTests {

    /**
     * Testing a new concept in writing to disk.
     *
     * Here, we're going to organize by the id.  Each item
     * will get its own file.  So Project 1 will get its own file, Project 2, etc.
     */
    @Test
    fun testSerializationNew_Project() {
        val dbDirectory = "build/db/testSerializationNew_Project/"
        File(dbDirectory).deleteRecursively()

        val projects = (1..10)
            .map { Project(ProjectId(it), ProjectName("$it")) }
            .toSet()

        val dbs = DatabaseSerialization(dbDirectory)

        val subDirectory = "project"
        dbs.serializeToDisk(projects, subDirectory)
        dbs.stop()

        assertDirectoryAndFilesAsExpected(dbDirectory, subDirectory, projects)
    }

    /**
     * If we already have a file in the directory and we remove
     * it and then add it back with new content, we should then
     * see the new content.
     *
     * Typically in the database, in memory, when we need to
     * update something, the only methods available are remove
     * and add.  So we remove, then we add, in order to update.
     */
    @Test
    fun testSerializationNew_Update() {
        val dbDirectory = "build/db/testSerializationNew_RemoveAndAdd/"
        val subDirectory = "project"
        File(dbDirectory).deleteRecursively()
        val dbs = DatabaseSerialization(dbDirectory)
        dbs.serializeToDisk(setOf(DEFAULT_PROJECT), subDirectory)

        val revisedProject = Project(DEFAULT_PROJECT.id, ProjectName("this is new"))
        dbs.serializeToDisk(setOf(revisedProject), subDirectory)
        dbs.stop()

        val text = File("$dbDirectory$subDirectory/${DEFAULT_PROJECT.id.value}$databaseFileSuffix").readText()
        val readProject = Project.deserialize(text)

        assertEquals(revisedProject, readProject)
    }

    @Test
    fun testSerializationNew_Delete() {
        val dbDirectory = "build/db/testSerializationNew_RemoveAndAdd/"
        val subDirectory = "project"
        File(dbDirectory).deleteRecursively()
        val dbs = DatabaseSerialization(dbDirectory)
        dbs.serializeToDisk(setOf(DEFAULT_PROJECT), subDirectory)

        dbs.deleteOnDisk(setOf(DEFAULT_PROJECT), subDirectory)
        dbs.stop()

        val doesExist = File("$dbDirectory$subDirectory/${DEFAULT_PROJECT.id.value}$databaseFileSuffix").exists()
        assertFalse("after deleting, the file should not exist", doesExist)
    }

    /**
     * Helper to make sure all the content is as expected
     */
    private fun assertDirectoryAndFilesAsExpected(dbDirectory : String, subDirectory : String, projects : Set<Project>) {
        val mutableProjects = projects.toMutableSet()
        File("$dbDirectory$subDirectory/")
            .walkTopDown()
            .filter { it.isFile }
            .forEach {
                val thisProject = Project.deserialize(it.readText())
                assertTrue("The file, ${it.name}, should start with the id of the project, ${thisProject.id}",
                    it.name.startsWith(thisProject.id.value.toString()))
                mutableProjects.remove(thisProject)
            }
        assertEquals("We should have removed every project in the list", 0, mutableProjects.size)
    }
}