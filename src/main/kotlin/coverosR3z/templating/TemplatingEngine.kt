package coverosR3z.templating

import kotlin.collections.*


class TemplatingEngine() {

    private val DEBUG = false

    fun render(input: String, mapping: Map<String, String>): String {
        val openSquiggles = input.count{ key -> key=='{' }
        val closedSquiggles = input.count{ key -> key=='}'}

        if (openSquiggles != closedSquiggles) {
            throw InvalidTemplateException(
                "Invalid syntax; amount of open and closed '{' brackets must match."
            )
        }
//        assert(openSquiggles == closedSquiggles) {
//            "Invalid templating syntax, number of open '{'s must match number of closed '}'s" }

        var regex = """\{\{(.*?)\}\}""".toRegex()
        val results = regex.findAll(input)
        val words = results.map{r -> r.groupValues[1]}

        var rendered = input

        for (word in words) {
            if (word in mapping.keys) {
                if (DEBUG) println("replacing instances of '$word' with '${mapping[word]}'")
                rendered = rendered.replace("{{$word}}", mapping[word]?: "")
                if (DEBUG) println(rendered)
            }
            else {
                throw InvalidTemplateException("Invalid syntax; all double bracketed values must have corresponding mappings.")
            }
        }

        return rendered
    }

}