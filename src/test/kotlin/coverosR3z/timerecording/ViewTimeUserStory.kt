package coverosR3z.timerecording

import coverosR3z.bddframework.UserStory

object ViewTimeUserStory : UserStory(
    key = "ViewTime",
    story =
    """
   As an employee
   I want to see the time entries I have previously entered
   So that I can confirm my time on work has been accounted correctly
    """
) {
    init {

        addScenario(
            "happy path - should be able to get my time entries on a date",

                "Given I have recorded some time entries",
                "When I request my time entries on a specific date",
                "Then I see all of them"
        )

        addScenario(
            "should be able to obtain all my time entries",

                "Given I have recorded some time entries",
                "When I request my time entries",
                "Then I see all of them"
        )

        addScenario(
            "there should be no entries on a given date if they have not been recorded yet",

                "Given no time entries were made on a day",
                "When I ask for the time entries of that day",
                "Then I am returned nothing"
        )
    }
}