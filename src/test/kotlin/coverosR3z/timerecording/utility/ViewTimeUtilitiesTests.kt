package coverosR3z.timerecording.utility

import coverosR3z.system.misc.A_RANDOM_DAY_IN_JUNE_2020
import coverosR3z.system.misc.DEFAULT_EMPLOYEE
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.api.ViewTimeAPI
import coverosR3z.timerecording.types.Time
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewTimeUtilitiesTests {

    val tru = FakeTimeRecordingUtilities()

    /**
     * This is going to be handy when we want to show the user how many
     * hours have been recorded for their current week.  This is acutely
     * useful when the week is split between time periods.
     */
    @Test
    fun `should properly calculate the number of hours from Sunday to Saturday`() {
        val expectedMinutes = 60 * 40
        val expectedTime = Time(expectedMinutes)
        tru.getTimeForWeekBehavior = { expectedTime }

        val result = ViewTimeAPI.calcHoursForWeek(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020, tru)

        assertEquals(expectedTime, result)
    }
}