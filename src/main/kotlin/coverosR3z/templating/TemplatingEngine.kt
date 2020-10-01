package coverosR3z.templating

import kotlin.collections.*


class TemplatingEngine() {

    fun render(input: String, mapping: Map<String, String>): String {
        val openSquiggles = input.count{ key -> key=='{' }
        val closedSquiggles = input.count{ key -> key=='}'}

        assert(openSquiggles == closedSquiggles) {
            "Invalid templating syntax, number of open '{'s must match number of closed '}'s" }

        var rendered = input
        var words = mutableListOf<String>()
        var word = ""

        var start = false
        var i=0;
        while (i < input.length-1) {
            val c = input[i]
            val next = input[i+1]

            if ("${c}${next}" == "{{") {
                start = true
                i += 2
            }
            if (start) {
                while (input[i] != '}') {
                    word += input[i]
                    i++
                }
                words.add(word)
                word = ""
                start = false
            }
            i++
        }

        println("Values in words are: ")
        println(words)

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