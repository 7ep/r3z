package coverosR3z.domainobjects

enum class StatusEnum {
    SUCCESS,
    INVALID_PROJECT,
    INVALID_EMPLOYEE,
    /**
     * Error message for logged in user does not match employee's time being entered
     */
    USER_EMPLOYEE_MISMATCH,
    NULL}

/**
 * This data holds the information that is relevant
 * to us after storing a time entry.
 */
data class RecordTimeResult(val status: StatusEnum = StatusEnum.NULL, val newTimeEntry : TimeEntry? = null)