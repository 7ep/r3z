package coverosR3z.persistence

import coverosR3z.misc.DEFAULT_PROJECT
import coverosR3z.persistence.types.ChangeTrackingSet
import coverosR3z.persistence.types.ChangeTrackingSet.DataAction.CREATE
import coverosR3z.persistence.types.ChangeTrackingSet.DataAction.DELETE
import coverosR3z.timerecording.types.Project
import coverosR3z.timerecording.types.ProjectId
import coverosR3z.timerecording.types.ProjectName
import org.junit.Assert.assertEquals
import org.junit.Test

class ChangeTrackingSetTests {

    private val newProject = Project(ProjectId(5), ProjectName("a"))

    /**
     * When a change is made to the [ChangeTrackingSet]
     * data structure, anything that was changed is
     * tracked, for purposes such as serializing those
     * items that are now different.
     *
     * This tests that when we create a new item, we
     * can get that as the new item we added
     */
    @Test
    fun shouldRecordModifiedValues_Create() {
        val projects = ChangeTrackingSet<Project>()
        projects.add(DEFAULT_PROJECT)

        val data = projects.modified.toSet()

        assertEquals(setOf(Pair(DEFAULT_PROJECT, CREATE)), data)
    }

    @Test
    fun shouldRecordModifiedValues_Remove() {
        val projects = ChangeTrackingSet<Project>()
        projects.add(DEFAULT_PROJECT)
        projects.clearModifications()
        projects.remove(DEFAULT_PROJECT)

        val data = projects.modified.toSet()

        assertEquals(setOf(Pair(DEFAULT_PROJECT, DELETE)), data)
    }

    /**
     * This is how we update data - remove the old and
     * add the new
     */
    @Test
    fun shouldRecordModifiedValues_RemoveAndAdd() {
        val projects = ChangeTrackingSet<Project>()
        projects.add(DEFAULT_PROJECT)
        projects.clearModifications()
        projects.remove(DEFAULT_PROJECT)
        projects.add(newProject)

        val data = projects.modified.toSet()

        assertEquals(setOf(Pair(DEFAULT_PROJECT, DELETE), Pair(newProject, CREATE)), data)
    }

    // what happens if we add the same thing twice?
    @Test
    fun shouldRecordModifiedValues_HandleAddingTwice() {
        val projects = ChangeTrackingSet<Project>()
        projects.add(DEFAULT_PROJECT)
        projects.add(DEFAULT_PROJECT)

        val data = projects.modified.toSet()

        assertEquals(setOf(Pair(DEFAULT_PROJECT, CREATE)), data)
    }

    // what happens if we use the addAll method
    @Test
    fun shouldRecordModifiedValues_AddAll() {
        val projects = ChangeTrackingSet<Project>()
        projects.addAll(listOf(DEFAULT_PROJECT))

        val data = projects.modified.toSet()

        assertEquals(setOf(Pair(DEFAULT_PROJECT, CREATE)), data)
    }

    /**
     * what happens if we use the addAll method
     * This one has two identical items to add
     */
    @Test
    fun shouldRecordModifiedValues_AddAllTwo() {
        val projects = ChangeTrackingSet<Project>()
        projects.addAll(listOf(DEFAULT_PROJECT, DEFAULT_PROJECT))

        val data = projects.modified.toSet()

        assertEquals(setOf(Pair(DEFAULT_PROJECT, CREATE)), data)
    }

    /**
     * what happens if we use the addAll method
     * This one has two different items to add
     */
    @Test
    fun shouldRecordModifiedValues_AddAllTwoDifferentItems() {
        val projects = ChangeTrackingSet<Project>()
        projects.addAll(listOf(DEFAULT_PROJECT, newProject))

        val data = projects.modified.toSet()

        assertEquals(setOf(Pair(DEFAULT_PROJECT, CREATE), Pair(newProject, CREATE)), data)
    }



}