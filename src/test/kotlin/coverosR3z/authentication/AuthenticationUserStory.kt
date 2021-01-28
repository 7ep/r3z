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
) {
    init {

        addScenario(
            "if I enter a bad password while logging in, I will be denied access",

                "Given I have registered,",
                "when I login with the wrong credentials,",
                "then the system denies me access."
        )

        addScenario(
            "if I enter too short a password while registering, it will disallow it",

                "Given I am not registered,",
                "when I register with too short of a password,",
                "then the system denies the registration on the basis of a bad password."
        )

        addScenario(
            "I should be able to log in once I'm a registered user",

                "Given I have registered,",
                "when I enter valid credentials,",
                "then the system knows who I am."
        )

        addScenario(
            "I should not be able to register a user if they are already registered",

                "Given I have previously been registered,",
                "when I try to register again,",
                "then the system records that the registration failed."
        )

        addScenario(
            "I should be able to register a user with a valid password",

                "Given I am not currently registered,",
                "when I register a new user,",
                "then the system records that the registration succeeded."
        )

        addScenario(
            "I cannot change someone else's time",

                "Given I am logged in as user alice and employees Sarah and Alice exist in the database,",
                "when I try to add a time-entry for Sarah,",
                "then the system disallows it."
        )


    }
}

