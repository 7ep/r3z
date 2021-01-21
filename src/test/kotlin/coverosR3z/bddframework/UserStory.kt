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
    private val scenarios : MutableList<BDDScenario> = mutableListOf()

    fun addScenario(description: String, steps: List<String>): BDDScenario {
        val newScenario = BDDScenario(description, steps, this)
        scenarios.add(newScenario)
        return newScenario
    }
}