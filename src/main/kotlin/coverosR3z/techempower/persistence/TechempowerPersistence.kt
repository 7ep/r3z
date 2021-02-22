package coverosR3z.techempower.persistence

import coverosR3z.logging.ILogger.Companion.logImperative
import coverosR3z.persistence.types.ChangeTrackingSet
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.techempower.types.World

class TechempowerPersistence(val pmd: PureMemoryDatabase) {

    fun getRow(id: Int) : World {
        return try {
            pmd.WorldDataAccess().read { worlds -> worlds.single { it.id == id } }
        } catch (ex: Throwable) {
            logImperative ("${ex.message}")
            World(0, 0)
        }
    }

    fun updateRows(rows: List<World>) {
        for (row in rows) {
            pmd.WorldDataAccess().actOn { it.update(row) }
        }
    }

    fun createRows(worlds: ChangeTrackingSet<World>) {
        for (row in worlds) {
            pmd.WorldDataAccess().actOn { it.add(row) }
        }
    }
}