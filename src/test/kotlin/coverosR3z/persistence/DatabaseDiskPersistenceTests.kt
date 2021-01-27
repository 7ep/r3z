package coverosR3z.persistence

import coverosR3z.misc.DEFAULT_PROJECT
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.misc.utility.decode
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.databaseFileSuffix
import coverosR3z.persistence.utility.DatabaseDiskPersistence.Companion.deserialize
import coverosR3z.timerecording.types.Project
import coverosR3z.timerecording.types.ProjectId
import coverosR3z.timerecording.types.ProjectName
import org.junit.Test
import java.io.File
import org.junit.Assert.*

class DatabaseDiskPersistenceTests {

    /**
     * Testing a new concept in writing to disk.
     *
     * Here, we're going to organize by the id.  Each item
     * will get its own file.  So Project 1 will get its own file, Project 2, etc.
     */
    @Test
    fun testPersistenceNew_Project() {
        val dbDirectory = "build/db/testPersistenceNew_Project/"
        File(dbDirectory).deleteRecursively()

        val projects = (1..10)
            .map { Project(ProjectId(it), ProjectName("$it")) }
            .toSet()

        val dbs = DatabaseDiskPersistence(dbDirectory)

        val subDirectory = "project"
        dbs.persistToDisk(projects, subDirectory)
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
    fun testPersistenceNew_Update() {
        val dbDirectory = "build/db/testPersistenceNew_Update/"
        val subDirectory = "project"
        File(dbDirectory).deleteRecursively()
        val dbs = DatabaseDiskPersistence(dbDirectory)
        dbs.persistToDisk(setOf(DEFAULT_PROJECT), subDirectory)

        val revisedProject = Project(DEFAULT_PROJECT.id, ProjectName("this is new"))
        dbs.persistToDisk(setOf(revisedProject), subDirectory)
        dbs.stop()

        val text = File("$dbDirectory$subDirectory/${DEFAULT_PROJECT.id.value}$databaseFileSuffix").readText()
        val readProject = Project.Deserializer().deserialize(text)

        assertEquals(revisedProject, readProject)
    }

    @Test
    fun testPersistenceNew_Delete() {
        val dbDirectory = "build/db/testPersistenceNew_Delete/"
        val subDirectory = "project"
        File(dbDirectory).deleteRecursively()
        val dbs = DatabaseDiskPersistence(dbDirectory)
        dbs.persistToDisk(setOf(DEFAULT_PROJECT), subDirectory)

        dbs.deleteOnDisk(setOf(DEFAULT_PROJECT), subDirectory)
        dbs.stop()

        val doesExist = File("$dbDirectory$subDirectory/${DEFAULT_PROJECT.id.value}$databaseFileSuffix").exists()
        assertFalse("after deleting, the file should not exist", doesExist)
    }

    @Test
    fun testDeserializerNew() {
        val projectSerialized = """{ id: 1 , name: myname }"""
         deserialize(projectSerialized, Project::class.java, Project.Companion) { entries ->
                val id = checkParseToInt(entries[Project.Companion.Keys.ID])
                Project(ProjectId(id), ProjectName(decode(checkNotNull(entries[Project.Companion.Keys.NAME]))))
            }
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
                val thisProject = Project.Deserializer().deserialize(it.readText())
                assertTrue("The file, ${it.name}, should start with the id of the project, ${thisProject.id}",
                    it.name.startsWith(thisProject.id.value.toString()))
                mutableProjects.remove(thisProject)
            }
        assertEquals("We should have removed every project in the list", 0, mutableProjects.size)
    }
}