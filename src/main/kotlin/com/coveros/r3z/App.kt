package com.coveros.r3z

import com.coveros.r3z.domainobjects.TimeEntry
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.http.ContentType
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import java.io.File

class App {

    val greeting: String
        get() {
            return "Hello world."
        }
}


fun main(args: Array<String>): Unit = io.ktor.server.jetty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        get("/html-freemarker") {
            call.respond(FreeMarkerContent("index.ftl", mapOf("data" to IndexData(listOf(1, 2, 3))), ""))
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }

    }
}

data class IndexData(val items: List<Int>)

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

fun recordTime(entry : TimeEntry) {
    val stringRepresentation = "ID: " + entry.user.id + "Time: " + entry.time.numberOfMinutes + "\nDetails: " + entry.details.value
    // do nothing for now.
}

