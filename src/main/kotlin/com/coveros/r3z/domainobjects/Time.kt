package com.coveros.r3z.domainobjects

/**
 * a length of time, in minutes
 */
data class Time(val numberOfMinutes : Int) {
    init {
        assert(numberOfMinutes > 0) { "number of minutes worked must be greater than 0, doesn't make sense otherwise"}
    }
}

