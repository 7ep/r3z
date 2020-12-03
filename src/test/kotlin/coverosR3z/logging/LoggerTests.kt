package coverosR3z.logging

import coverosR3z.misc.getTime
import org.junit.Assert.assertTrue
import org.junit.Test

class LoggerTests {

    /**
     * Here, we will see whether our system runs faster with the logs
     * turned off, if the input to the logging is simply passed as
     * a parameter or if by lambda
     *
     * It's race time.
     *
     * And the result isn't even close.
     *
     * In conclusion: If you have something more compute-intensive that's only
     * being done on behalf of a logging statement, it will be a very smart
     * move to put it in as a lambda, for when we turn off the logging.
     *
     * What we are seeing in this experiment is that we've turned off the
     * tracing log, but even so, the code that goes in the parens version
     * of the method takes *way* longer than the one that accepts a lambda.
     */
    @Test
    fun testShouldLambdaRunFaster_lambdaOrParens_PERFORMANCE() {
        logSettings[LogTypes.TRACE] = false
        val (timeParens, _) = getTime {repeat(1000) {
            logTrace("abc".repeat(100).replace("c", "b").replace("b", "a"))
        }}
        val (timeLambda, _) = getTime {repeat(1000) {
            logTrace{"abc".repeat(100).replace("c", "b").replace("b", "a")}
        }}
        println("timeLambda: $timeLambda, timeParens: $timeParens")
        assertTrue("timeLambda: $timeLambda, timeParens: $timeParens" , timeLambda < timeParens)
    }


}