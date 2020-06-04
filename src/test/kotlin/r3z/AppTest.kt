package r3z

import org.junit.Test
import kotlin.test.assertEquals


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
     * For someone working at a company, let's say they need to record
     * a log of their hours working on different projects.
     * The simplest approach...
     * This is imperative - we are *storing* data, not manipulating it (much)
     * so our testing becomes more difficult.  We have to *do* a thing, then
     * run separate code to check that the thing was *done*
     */
//    @Test fun `record time for someone`() {
//
//        recordTime(timeEntry);
//
//        printTime(user)
//    }

    /**
     * Now, this is something functional.  On the input, we want various
     * data, on the output we want a nice data object that has the relevant
     * interrelated data
     */
//    @Test fun `make time entry`() {
//        val user = User()
//        val project = Project()
//        val time = Time()
//        val log = Log()
//        makeDataEntry(user, project, time, log)
//    }


}
