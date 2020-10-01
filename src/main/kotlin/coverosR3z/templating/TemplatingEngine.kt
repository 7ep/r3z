package coverosR3z.templating

import kotlin.collections.*


class TemplatingEngine() {

    fun render(input: String, mapping: Map<String, String>): String {
        var rendered = input
        var flag = 0
        var words = mutableListOf<String>()
        var word = ""
        for (s in input) {
            println(s)
            if (flag == 2 && s != '}') {
                word += s
            }
            if (s == '{') {
                flag++
            }
            if (s == '}') {
                flag = 0
                println(word)
                words.add(word)
                word = ""
            }
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