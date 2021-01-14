package coverosR3z.bdd

class BDDScenario(private val description: String, steps: List<String>, private val userStory: UserStory) {

    private val renderedSteps = mutableListOf<String>()

    init {
        renderedSteps.addAll(steps.map{ "<not-done>$it</not-done>" } )
    }

    fun markDone(step: String) {
        for(i in renderedSteps.indices) {
            renderedSteps[i] = renderedSteps[i].replace("<not-done>$step</not-done>", "<done>$step</done>")
        }
        println(userStory.story)
        println()
        println(description)
        renderedSteps.forEach { println(it) }
    }

}
