package coverosR3z.techempower.utility

import coverosR3z.logging.ILogger.Companion.getCurrentMillis
import coverosR3z.persistence.types.ChangeTrackingSet
import coverosR3z.server.types.ServerData
import coverosR3z.techempower.types.World
import kotlin.random.Random

interface ITechempowerUtilities {
    fun getRow(id: Int): World
    fun updateRows(rows: List<World>)

    companion object {
        val rand = Random(getCurrentMillis())

        fun generateWorlds() : ChangeTrackingSet<World> {
            val worldsSet = ChangeTrackingSet<World>()
            (1..10_000).forEach { worldsSet.addWithoutTracking(World(it, 0)) }
            return worldsSet
        }
    }

    fun getRandomRows(numQueries: Int, sd: ServerData): List<World>
    fun randomizeAndUpdate(rows: List<World>, sd: ServerData): List<World>
    fun addRows(worlds: ChangeTrackingSet<World>)
}