package coverosR3z.timerecording

import coverosR3z.bddframework.UserStory

object RecordTimeUserStory : UserStory(
    key = "RecordTime",
    story =
    """
        As an employee, Andrea,
        I want to record my time
        So that I am easily able to document my time in an organized way
    """
) {
    init {

        addScenario(
            "A employee enters six hours on a project with copious notes",

                "Given I have worked 6 hours on project A on Monday with a lot of notes,",
                "when I enter in that time,",
                "then the system indicates it has persisted the new information."
        )

        addScenario(
            "A employee has already entered 24 hours for the day, they cannot enter more time on a new entry",

                "given the employee has already entered 24 hours of time entries before,",
                "when they enter in a new time entry for one hour,",
                "then the system disallows it."
        )
    }
}