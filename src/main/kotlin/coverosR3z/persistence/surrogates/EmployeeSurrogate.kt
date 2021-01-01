package coverosR3z.persistence.surrogates

import coverosR3z.domainobjects.Employee
import coverosR3z.domainobjects.EmployeeId
import coverosR3z.domainobjects.EmployeeName
import coverosR3z.exceptions.DatabaseCorruptedException
import coverosR3z.misc.checkParseToInt
import coverosR3z.misc.decode
import coverosR3z.misc.encode

/**
 * A surrogate. See longer description for another surrogate at [TimeEntrySurrogate]
 */
data class EmployeeSurrogate(val id: Int, val name: String) {

    fun serialize(): String {
        return """{ id: $id , name: ${encode(name)} }"""
    }

    companion object {

        fun deserialize(str: String): EmployeeSurrogate {
            try {
                val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap{it.groupValues}.toList()
                val id = checkParseToInt(groups[1])
                return EmployeeSurrogate(id, decode(groups[3]))
            } catch (ex : Throwable) {
                throw DatabaseCorruptedException("Unable to deserialize this text as employee data: $str", ex)
            }
        }

        fun deserializeToEmployee(str: String) : Employee {
            return fromSurrogate(deserialize(str))
        }

        fun toSurrogate(e : Employee) : EmployeeSurrogate {
            return EmployeeSurrogate(e.id.value, e.name.value)
        }

        private fun fromSurrogate(es: EmployeeSurrogate) : Employee {
            return Employee(EmployeeId(es.id), EmployeeName(es.name))
        }

    }
}