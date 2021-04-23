package coverosR3z.timerecording.exceptions

/**
 * Represents when the time entries exceed the boundaries of
 * the time period allowed.  For example, if the time
 * period is 2 weeks and we calculate that the earliest
 * and latest time entry are more than 2 weeks apart.
 */
class InvalidTimePeriodException : Exception()