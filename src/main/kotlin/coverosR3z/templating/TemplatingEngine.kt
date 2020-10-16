package coverosR3z.templating

import coverosR3z.logging.logDebug
import kotlin.collections.*


class TemplatingEngine {

    fun render(input: String, mapping: Map<String, String>): String {
        val openSquiggles = input.count{ key -> key=='{' }
        val closedSquiggles = input.count{ key -> key=='}'}

        if (openSquiggles != closedSquiggles) {
            throw InvalidTemplateException(
                "Invalid syntax; amount of open and closed '{' brackets must match."
            )
        }

        val regex = """\{\{(.*?)}}""".toRegex()
        val results = regex.findAll(input)
        val words = results.map{r -> r.groupValues[1]}

        var rendered = input

        for (word in words) {
            if (word in mapping.keys) {
                logDebug("replacing instances of '$word' with '${mapping[word]}'")
                rendered = rendered.replace("{{$word}}", mapping[word]?: "")
                logDebug(rendered)
            }
            else {
                throw InvalidTemplateException("Invalid syntax; all double bracketed values must have corresponding mappings.")
            }
        }

        return rendered
    }

}