package com.coveros.r3z

import com.coveros.r3z.domainobjects.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppTest {

    private val threeHoursFifteen = (3 * 60) + 15

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
     * For someone working at a company, let's say they need to record
     * a log of their hours working on different projects.
     * The simplest approach...
     * This is imperative - we are *storing* data, not manipulating it (much)
     * so our testing becomes more difficult.  We have to *do* a thing, then
     * run separate code to check that the thing was *done*
     */
    @Test fun `record time for someone`() {
        val user = User(1, "")
        val time = Time(300)
        val project = Project(1, "test")
        val details = Details("testing, testing")

        val entry = TimeEntry(user, project, time, details)
        recordTime(entry)

//        printTime(user)
    }

    /**
     * Now, this is something functional.  On the input, we want various
     * data, on the output we want a nice data object that has the relevant
     * interrelated data
     */
    @Test fun `make time entry`() {
        val expectedDataEntry = generateDataEntry()
        val user = User(1, "")
        val project = Project(1, "a")
        val time = Time(threeHoursFifteen)
        val details = Details("sample comment")
        val actualDataEntry : TimeEntry = makeDataEntry(user, project, time, details)
        assertEquals(expectedDataEntry, actualDataEntry)
    }

    @Test fun `a user should have a unique integer identifier`() {
        val user = User(1, "")
        assertEquals(1, user.id)
    }

    @Test fun `a user should have a name`() {
        val name = "this is my name bro"
        val id : Long = 1

        val user = User(id, name)

        assertEquals(id, user.id)
        assertEquals(name, user.name)
    }

    @Test fun `a time should have a decimal representation of its value`() {
        val time = Time(threeHoursFifteen)
        assertEquals(threeHoursFifteen, time.numberOfMinutes)
    }

    @Test fun `a project should have a name and an id`() {
        val project = Project(1, "some project name")
        assertEquals(1, project.id)
        assertEquals("some project name", project.name)
    }

    @Test fun `details should have a string representation` () {
        val actual = Details("Testing, testing")
        val expected = "Testing, testing"
        assertEquals(expected, actual.value)
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
        assertFailsWith<AssertionError> { Details("way too long wayyyy too long  ".repeat(30)) }
    }

    @Test fun `there should be no difference between details with no args and details with ""`() {
        val actual = Details("")
        val expected = Details()
        assertEquals(expected, actual)
    }

    /**
     * A helper method to create data entries for timekeeping
     */
    private fun generateDataEntry() : TimeEntry {
        return TimeEntry(User(1, ""), Project(1, "a"), Time(threeHoursFifteen), Details())
    }

    private fun makeDataEntry(user: User, project: Project, time: Time, details: Details) : TimeEntry {
        return TimeEntry(User(1, ""), Project(1, "a"), Time(threeHoursFifteen), Details())
    }


}
