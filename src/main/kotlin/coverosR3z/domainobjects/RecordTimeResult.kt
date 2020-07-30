package coverosR3z.domainobjects

enum class StatusEnum {
    SUCCESS,
    FAILURE,
    INVALID_PROJECT,
    INVALID_EMPLOYEE,
    NULL}

/**
 * This data holds the information that is relevant
 * to us after storing a time entry.
 */
data class RecordTimeResult(val id: Long?, val status: StatusEnum = StatusEnum.NULL)