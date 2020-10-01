package coverosR3z.templating

import kotlin.collections.*


class TemplatingEngine() {

    fun render(input: String, mapping: Map<String, String>): String {
        val openSquiggles = input.count{ key -> key=='{' }
        val closedSquiggles = input.count{ key -> key=='}'}

        assert(openSquiggles == closedSquiggles) {
            "Invalid templating syntax, number of open '{'s must match number of closed '}'s" }

        var regex = """\{\{(.*?)\}\}""".toRegex()
        val results = regex.findAll(input)
        val words = results.map{r -> r.groupValues[1]}

        var rendered = input

        for (word in words) {
            if (word in mapping.keys) {
                println("replacing instances of '$word' with '${mapping[word]}'")
                rendered = rendered.replace("{{$word}}", mapping[word]?: "")
                println(rendered)
            }
        }

        return rendered
    }

}