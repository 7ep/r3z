package coverosR3z.templating

class TemplatingEngine() {

    fun render(input: String): String {
        val match = input.replace("{{value}}" , "matt")
        return match
    }

}