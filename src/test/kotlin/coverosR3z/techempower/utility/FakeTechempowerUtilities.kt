package coverosR3z.techempower.utility

import coverosR3z.persistence.types.ChangeTrackingSet
import coverosR3z.server.types.ServerData
import coverosR3z.techempower.types.World

class FakeTechempowerUtilities : ITechempowerUtilities {
    override fun getRow(id: Int): World {
        TODO("Not yet implemented")
    }

    override fun updateRows(rows: List<World>) {
        TODO("Not yet implemented")
    }

    override fun getRandomRows(numQueries: Int, sd: ServerData): List<World> {
        TODO("Not yet implemented")
    }

    override fun randomizeAndUpdate(rows: List<World>, sd: ServerData): List<World> {
        TODO("Not yet implemented")
    }

    override fun addRows(worlds: ChangeTrackingSet<World>) {
        TODO("Not yet implemented")
    }
}