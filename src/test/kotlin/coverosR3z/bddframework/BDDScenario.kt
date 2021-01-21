package coverosR3z.bddframework

import java.io.File

/**
 * A scenario, part of a [UserStory].
 *
 * This represents a concrete example of the [UserStory] in action.
 *
 * It should be simple, clear, testable, small, and for our purposes here,
 * testable by automation.
 *
 * @param shortDesc a short one-line description of this scenario
 * @param steps the multiple steps of the scenario.  Each line (each step) is a
 *              new entry in the list.
 */
class BDDScenario(private val shortDesc: String, steps: List<String>, private val userStory: UserStory) {

    /**
     * Searches for anything in a string that isn't alphanumeric
     */
    private val onlyAlphaNumerics = "[^a-zA-Z0-9]".toRegex()

    private val renderedSteps = mutableListOf<String>()

    init {
        renderedSteps.addAll(steps.map{ "<not-done>$it</not-done>" } )
    }

    fun markDone(step: String) {
        for(i in renderedSteps.indices) {
            renderedSteps[i] = renderedSteps[i].replace("<not-done>$step</not-done>", "<done>$step</done>")
        }
        writeBDDFile()
    }

    fun writeBDDFile() {
        File("build/bdd/${userStory.key}/").mkdirs()

        val storyFile = File("build/bdd/${userStory.key}/story.html")
        if (!storyFile.exists()) {
            storyFile.writeText("<userstory>\n${userStory.story.trimIndent()}\n</userstory>")
        }

        val scenarioName = onlyAlphaNumerics.replace(shortDesc, "_")
        File("build/bdd/${userStory.key}/scenario_$scenarioName.html").writeText(
            "<scenario>$shortDesc</scenario>\n" + renderedSteps.joinToString(
                "\n"
            )
        )
    }

}
