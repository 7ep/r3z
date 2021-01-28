package coverosR3z.bddframework

/**
 * Represents a user story, a conception of a feature which includes the following:
 * who the feature benefits
 * how it benefits them
 * and the feature.
 *
 * For example,
 *
 * As a librarian
 * I want to check books out to users
 * So that I can help them enjoy our library's books
 *
 * See also [BDDScenario]
 *
 * @param story the entire user story text
 * @param key a short and unique token - one or maybe two words, without
 *                  spaces - to help label this user story.  For example, RecordTime
 */
abstract class UserStory(val key : String, val story: String) {
    private val scenarios : MutableMap<String, BDDScenario> = mutableMapOf()

    /**
     * Adds a new scenario for this user story
     * @param description a sentence-length description of the scenario
     * @param steps one or more steps that must be satisfied for this scenario to pass
     */
    fun addScenario(description: String, vararg steps: String): BDDScenario {
        val newScenario = BDDScenario(description, steps.toList(), this)
        // write the initial (nothing yet done) scenario, if nothing is marked done
        newScenario.writeBDDFile()
        scenarios[description] = newScenario
        return newScenario
    }

    fun getScenario(description : String) : BDDScenario {
        return checkNotNull(scenarios[description])
        {"The scenario provided, \"$description\", did not match any BDD scenario.\n" +
                "All the scenarios registered are ${scenarios.keys.joinToString(",")}"}
    }
}