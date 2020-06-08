
package r3z

class App {

    val greeting: String
        get() {
            return "Hello world."
        }
}

fun main(args: Array<String>) {
    println(App().greeting)
}

fun mattBaddassAdder(num1 : Int, num2 : Int) : Int {
    val result = num1 + num2
    return result
}

fun byronBaddassSinglePurposePow(num : Int) : Int {
    return num * num
}

fun mockMeBaby(input : String) : String {
    var result = ""
    var tracker = false
    for(letter in input) {
        if (letter == ' ') {
            result += ' '
            continue
        }
        if (tracker) {
            result += letter.toUpperCase()
            tracker = false
        } else {
            result += letter.toLowerCase()
            tracker = true
        }
    }
    return result
}

/**
 * Takes naughty potty-mouth text and takes that shit out
 */
fun restrictMySpeech(text: String) : String {
    val bannedWords = listOf("fuck", "shit")
    val regexes : MutableList<String> = mutableListOf()
    for (word in bannedWords) {
        val partialRegex = word.toCharArray()
                .joinToString(".?") +  // I want to potentially handle a single-char divider between letters
                "[^ ]* ?"              // zero or more "not-a-space", ending at a space
        regexes.add(partialRegex)
    }
    val regex = Regex(regexes.joinToString("|"), RegexOption.IGNORE_CASE)
    return regex.replace(text, "").trim()
}