package com.coveros.r3z

import com.coveros.r3z.domainobjects.*
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.Assert.*
import org.junit.Test


class AppTest {

    /**
     * What does a default (empty) Details look like
     */
    @Test fun `details should by default contain an empty string`() {
        val actual = Details()
        val expected = ""

        assertEquals(expected, actual.value)
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
