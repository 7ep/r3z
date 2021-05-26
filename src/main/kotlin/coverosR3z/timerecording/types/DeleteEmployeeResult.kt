package coverosR3z.timerecording.types

enum class DeleteEmployeeResult {
    SUCCESS,

    /**
     * we return this if our code to delete an item
     * at the persistence layer returns false - which
     * probably means the item didn't exist
     */
    DID_NOT_DELETE,

    /**
     * If a user has been registered to this
     * employee, we cannot delete the employee
     */
    TOO_LATE_REGISTERED,
}