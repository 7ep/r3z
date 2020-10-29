package coverosR3z.domainobjects

import kotlinx.serialization.Serializable

/**
 * a length of time, in minutes
 */
@Serializable
data class Time(val numberOfMinutes : Int) {
    init {
        require(numberOfMinutes > 0) {"Doesn't make sense to have zero or negative time"}
        require(numberOfMinutes <= 60*24) {"Entries do not span multiple days, thus must be <=24 hrs"}
    }
}

