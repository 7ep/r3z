package coverosR3z.techempower.api

import coverosR3z.logging.getCurrentMillis
import coverosR3z.server.types.*
import coverosR3z.techempower.types.World
import kotlin.random.Random


class TechempowerAPI {

    companion object : GetEndpoint {

        override fun handleGet(sd: ServerData): PreparedResponseData {
            val numQueries = try {
                val numQueries = Integer.parseInt(sd.ahd.queryString["queries"])
                when {
                    numQueries < 0 -> 1
                    numQueries > 500 -> 500
                    else -> numQueries
                }
            } catch (ex : Throwable) {
                1
            }

            val rows : List<World> = sd.bc.tu.getRandomRows(numQueries, sd)
            val newRows : List<World> = sd.bc.tu.randomizeAndUpdate(rows, sd)

            val jsonRows = newRows.joinToString(",") { """{"id":${it.id}, "randomNumber":${it.randomNumber}}""" }
            val returnBody = "[$jsonRows]"

            return PreparedResponseData(
                returnBody,
                StatusCode.OK,
                listOf(ContentType.APPLICATION_JSON.value))
        }

        override val path: String
            get() = "updates"

    }
}