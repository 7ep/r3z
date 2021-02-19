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

    }
}