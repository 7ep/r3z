package coverosR3z.templating

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import coverosR3z.templating.*

class TemplatingTests {

    val te = TemplatingEngine()

    @Test
    fun `should be able to generate html with a variable substituted`() {
        var expected = "<body>hello matt</body>"
        var pre = "<body>hello {{value}}</body>"
        var actual = te.render(pre)

        assertEquals(expected, actual)
    }
}