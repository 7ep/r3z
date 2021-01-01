package coverosR3z.persistence.surrogates

import coverosR3z.domainobjects.*
import coverosR3z.exceptions.DatabaseCorruptedException
import coverosR3z.misc.checkParseToInt
import coverosR3z.misc.decode
import coverosR3z.misc.encode
import coverosR3z.persistence.ConcurrentSet

/**
 * Don't be alarmed, this is just a sneaky way to create far smaller text
 * files when we serialize [TimeEntry].
 *
 * Instead of all the types, we just do what we can to store the raw
 * values in a particular order, which cuts down the size by like 95%
 *
 * So basically, right before serializing we convert our list of time
 * entries to this, and right after deserializing we convert this to
 * full time entries. Win-Win!
 *
 * We do throw a lot of information away when we convert this over.  We'll
 * see if that hurts our performance.
 *
 * @param i the integer identifier of the Time Entry
 * @param e the [Employee] id
 * @param p the [Project] id
 * @param t the [Time], in minutes
 * @param d the [Date], as epoch days
 * @param dtl the [Details], as a string
 */
data class TimeEntrySurrogate(val i: Int, val e: Int, val p: Int, val t : Int, val d : Int, val dtl: String) {

    fun serialize(): String {
        return """{ i: $i , e: $e , p: $p , t: $t , d: $d , dtl: ${encode(dtl)} }"""
    }

    companion object {

        fun deserialize(str: String): TimeEntrySurrogate {
            try {
                val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap{it.groupValues}.toList()
                val id = checkParseToInt(groups[1])
                val empId = checkParseToInt(groups[3])
                val projId = checkParseToInt(groups[5])
                val time = checkParseToInt(groups[7])
                val date = checkParseToInt(groups[9])
                val details = decode(groups[11])
                return TimeEntrySurrogate(id, empId, projId, time, date, details)
            } catch (ex : Throwable) {
                throw DatabaseCorruptedException("Unable to deserialize this text as time entry data: $str", ex)
            }
        }

        fun deserializeToTimeEntry(str: String, employee: Employee, projects: ConcurrentSet<Project>) : TimeEntry {
            return fromSurrogate(deserialize(str), employee, projects)
        }

        fun toSurrogate(te : TimeEntry) : TimeEntrySurrogate {
            return TimeEntrySurrogate(
                te.id,
                te.employee.id.value,
                te.project.id.value,
                te.time.numberOfMinutes,
                te.date.epochDay,
                te.details.value
            )
        }

        private fun fromSurrogate(te: TimeEntrySurrogate, employee: Employee, projects: ConcurrentSet<Project>) : TimeEntry {
            val project = try {
                projects.single { it.id == ProjectId(te.p) }
            } catch (ex : NoSuchElementException) {
                throw DatabaseCorruptedException("Unable to find a project with the id of ${te.p}.  Project set size: ${projects.size()}")
            }
            return TimeEntry(
                te.i,
                employee,
                project,
                Time(te.t),
                Date(te.d),
                Details(te.dtl)
            )

        }

    }
}