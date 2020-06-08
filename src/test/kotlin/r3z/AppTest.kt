package r3z

import org.junit.Test
import r3z.domainobjects.*
import kotlin.test.assertEquals


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
    @Test fun `make time entry`() {
        val expectedDataEntry = generateDataEntry()
        val user = User(1)
        val project = Project()
        val time = Time(threeHoursFifteen)
        val log = Log()
        val actualDataEntry : DataEntry = makeDataEntry(user, project, time, log)
        assertEquals(expectedDataEntry, actualDataEntry)
    }

    @Test fun `a user should have a unique integer identifier`() {
        val user = User(1)
        assertEquals(1, user.id)
    }

    @Test fun `a time should have a decimal representation of its value`() {
        val time = Time(threeHoursFifteen)
        assertEquals(threeHoursFifteen, time.numberOfMinutes)
    }

    /**
     * A helper method to create data entries for timekeeping
     */
    private fun generateDataEntry() : DataEntry {
        return DataEntry(User(1), Project(), Time(threeHoursFifteen), Log())
    }

    private fun makeDataEntry(user: User, project: Project, time: Time, log: Log) : DataEntry {
        return DataEntry(User(1), Project(), Time(threeHoursFifteen), Log())
    }


}
