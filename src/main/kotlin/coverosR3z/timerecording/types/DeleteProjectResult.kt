package coverosR3z.timerecording.types

enum class DeleteProjectResult {
    /**
     * Successfully deleted the project
     */
    SUCCESS,

    /**
     * Cannot delete the project because it is currently used in
     * a time entry somewhere
     */
    USED
}