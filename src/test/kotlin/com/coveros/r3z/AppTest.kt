package com.coveros.r3z

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.timerecording.recordTime
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.Assert.*
import org.junit.Test


class AppTest {


    @Test fun `hey there dude, add two numbers will ya?`() {
        val result = mattBaddassAdder(2, 2)
        assertEquals(4, result)
    }

    @Test fun `square a number`() {
        val result = byronBaddassSinglePurposePow(3)
        assertEquals(9, result)
    }

    @Test fun `make fun of people`() {
        val expected = "nO yOu ArE"
        val input = "no you are"

        val result = mockMeBaby(input)

        assertEquals(expected, result)
    }

    @Test fun `censor naughty potty words`() {
        val expected = "I love that kotlin"
        val input = "I fucking love that kotlin shit"
        val result = restrictMySpeech(input)

        assertEquals(expected, result)
    }

    @Test fun `censor naughty potty words with extreme vigilance and prejudice`() {
        val expected = "I love that kotlin"
        val input = "I f.u.c.king shit shitting FUCK love that kotlin shit"
        val result = restrictMySpeech(input)

        assertEquals(expected, result)
    }

    /**
     * What does a default (empty) Details look like
     */
    @Test fun `details should by default contain an empty string`() {
        val actual = Details()
        val expected = ""

        assertEquals(expected, actual.value)
    }

    /**
     * Crazy-long details are shunned
     */
    @Test fun `details shouldn't be too long`() {
        assertThrows(AssertionError::class.java) {Details("way too long wayyyy too long  ".repeat(30))}
    }

    @Test fun `there should be no difference between details with no args and details with ""`() {
        val actual = Details("")
        val expected = Details()
        assertEquals(expected, actual)
    }

    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }

    @Test
    fun testTemplatePage() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/login").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertTrue(response.content!!.contains("Hi everybody!"))
            }
        }
    }

}
