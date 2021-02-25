package coverosR3z.techempower.utility

import coverosR3z.persistence.types.ChangeTrackingSet
import coverosR3z.server.types.ServerData
import coverosR3z.techempower.persistence.TechempowerPersistence
import coverosR3z.techempower.types.World
import coverosR3z.techempower.utility.ITechempowerUtilities.Companion.rand

class TechempowerUtilities(val tp: TechempowerPersistence) : ITechempowerUtilities {

    override fun getRow(id: Int) : World {
        return tp.getRow(id)
    }

    override fun getRandomRows(numQueries: Int, sd: ServerData): List<World> {
        return (1..numQueries).map { sd.bc.tu.getRow(rand.nextInt(1, 10_000)) }
    }

    override fun updateRows(rows: List<World>) {
        tp.updateRows(rows)
    }

    override fun randomizeAndUpdate(rows: List<World>, sd: ServerData): List<World> {
        val newRows = rows.map{ World(it.id, rand.nextInt(1, 100_000)) }
        updateRows(newRows)
        return newRows
    }

    override fun addRows(worlds: ChangeTrackingSet<World>) {
        tp.createRows(worlds)
    }

}