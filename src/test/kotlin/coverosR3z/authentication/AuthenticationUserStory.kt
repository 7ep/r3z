package coverosR3z.authentication

import coverosR3z.bddframework.UserStory

object AuthenticationUserStory : UserStory(
    key = "Authentication",
    story =
    """
        As an employee,
        I want to be able to securely use the system,
        so that I know my time entries are confidential and cannot be manipulated by others
    """
)