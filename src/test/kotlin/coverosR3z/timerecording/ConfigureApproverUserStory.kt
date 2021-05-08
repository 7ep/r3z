package coverosR3z.timerecording

import coverosR3z.bddframework.UserStory

object ConfigureApproverUserStory : UserStory(
    key = "ConfigureApprover",
    story =
    """
   As a company executive
   I want to enable certain people to approve other people's time
   So that my managers can verify the activities of only their reports
    """
) {
    init {

        addScenario(
            "Approver - Should be possible to assign approvees to an approver",

                "Given there is a manager, Bob, with three reports - Jona, Matt, and Mitch",
                "when I configure approvals properly",
                "then he can approve any of their time"
        )

        addScenario(
            "Approver - Should not be possible to approve unassigned reports",

                "Given there is a manager, Bob, who doesn't directly or indirectly manage Tom",
                "when I configure approvals properly",
                "then he cannot approve Tom's time"
        )

        addScenario(
            "Approver - Should be possible to approve sub-reports",

                "Given Alice manages Bob, and Bob manages Carol",
                "when I configure approvals properly",
                "then Alice can approve Carol's time"
        )

        addScenario(
            "Approver - Should be possible to remove a report",

                "Given Alice manages Bob and Carol, but Carol just left the company",
                "when I configure the approvals to remove Carol from Bob",
                "then Bob can no longer approve Carol's time"
        )

    }
}
