package coverosR3z.timerecording

import coverosR3z.bddframework.UserStory

object EditTimeUserStory : UserStory(
    key = "EditTime",
    story =
    """
   As an employee
   I want to edit existing time entries I have previously entered
   So that I can correct mistakes
    """
) {
    init {

        addScenario(
            "editTime - An employee should be able to edit the number of hours worked from a previous time entry",

                "Given Andrea has a previous time entry with 1 hour,",
                "when she changes the entry to two hours,",
                "then the system indicates the two hours was persisted"
        )

    }
}
