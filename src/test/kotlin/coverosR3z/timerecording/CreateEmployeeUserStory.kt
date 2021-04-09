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