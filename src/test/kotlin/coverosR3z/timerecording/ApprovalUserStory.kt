package coverosR3z.timerecording

import coverosR3z.bddframework.UserStory

object ApprovalUserStory : UserStory(
    key = "Approval",
    story =
    """
   As an approver
   I want to approve time entries
   In order to indicate I have reviewed them and found them accurate
    """
) {
    init {

        addScenario(
            "Approval - An approver should be able to approve submitted time",

                "Given an employee submitted their time,",
                "when I approve it",
                "then the timesheet is approved"
        )

        addScenario(
            "Approval - An approver should be able to unapprove submitted time",

                "Given an employee needs to make some changes to previously approved time",
                "when I unapprove that time period",
                "then the timesheet is unapproved"
        )

        addScenario(
            "Approval - An approver should not be able to approve unsubmitted time",

                "Given an employee had not submitted their time,",
                "when I try to approve it",
                "then the approval status remains unchanged"
        )

        addScenario(
            "Approval - approved time periods cannot be unsubmitted",

                "Given a time period had been approved",
                "when the employee tries to unsubmit their time",
                "then they are unable to do so"
        )

        addScenario(
            "Approval - unapproved time periods can be unsubmitted",

                "Given a time period had previously been approved but then unapproved",
                "when the employee tries to unsubmit their time",
                "then they are able to do so"
        )

    }
}
