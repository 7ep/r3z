package coverosR3z.timerecording.types

import coverosR3z.misc.types.Date

data class TimePeriod(val start: Date, val end: Date) {

    /**
     * Helper function to determine whether a provided date falls within a period
     */
    fun contains(date: Date) : Boolean {
        return date in start .. end
    }
}
