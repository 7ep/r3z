package coverosR3z.bdd

abstract class UserStory(val story: String) {
    private val scenarios : MutableList<BDDScenario> = mutableListOf()

    fun addScenario(description: String, steps: List<String>): BDDScenario {
        val newScenario = BDDScenario(description, steps, this)
        scenarios.add(newScenario)
        return newScenario
    }
}