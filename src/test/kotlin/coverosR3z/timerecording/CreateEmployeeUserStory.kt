package coverosR3z.timerecording

import coverosR3z.bddframework.UserStory

object CreateEmployeeUserStory : UserStory(
    key = "CreateEmployee",
    story =
    """
        As an administrator, Adrian,
        I want to create new employees,
        So that I can allow new employees to track time
    """
) {
    init {

        addScenario(
            "createEmployee - I should be able to create an employee",

                "Given the company has hired a new employee, Andrea,",
                "when I add her as an employee,",
                "then the system persists the data."
        )

        /*
        Deleting elements in the system is reserved for those situations
        where it wasn't really meant to be done in the first place.  Later,
        when actions have taken place (registering a user, creating time entries,
        etc), it would mean a cascade of deletions that pose a much greater risk of
        data corruption.  In that case, if the admins want to remove a user, it
        would be preferable to do so with a simpler active/inactive flag
         */
        addScenario(
            "createEmployee - I should be able to delete an employee",

                "Given I accidentally added a new employee",
                "when I delete that employee",
                "then the employee no longer exists in the system"
        )

        // see notes on "createEmployee - I should be able to delete an employee"
        addScenario(
            "createEmployee - I should not be able to delete a registered employee",

                "Given a new employee was created and they registered a user",
                "when I try deleting that employee",
                "then I am prevented from doing so"
        )

        addScenario(
            "createEmployee - an invitation is created",

                "Given the company has hired a new employee, Hank,",
                "when I add him as an employee,",
                "an invitation is created for him"
        )

        addScenario(
            "createEmployee - using an invitation to register a user",

                "Given an invitation has been created for a new employee, Hank",
                "When he uses that invitation",
                "Then he is able to register"
        )

    }
}