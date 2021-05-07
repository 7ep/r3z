package coverosR3z.timerecording

import coverosR3z.bddframework.UserStory

object CreateProjectUserStory : UserStory(
    key = "CreateProject",
    story =
    """
        As an administrator, Adrian,
        I want to create new projects,
        So that time entries can be assigned to different projects
    """
) {
    init {

        addScenario(
            "CreateProject - I should be able to create a project",

                "Given my company has different projects we're working on",
                "when I create a project,",
                "then that project is available for tracking time"
        )

        addScenario(
            "CreateProject - I should be able to delete a project",

                "Given a newly created project hasn't been used yet for time",
                "when I delete that project,",
                "then that project gone from the system"
        )

        addScenario(
            "CreateProject - I should not be able to delete a used project",

                "Given a project has been used for time entries",
                "when I try to delete that project,",
                "then the system disallows it"
        )


    }
}