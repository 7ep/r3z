package coverosR3z.templating

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import coverosR3z.templating.*
import org.junit.Assert
import java.lang.AssertionError

class TemplatingTests {

    val te = TemplatingEngine()

    @Test
    fun `should be able to generate html with a variable substituted`() {
        var expected = "<body>hello matt</body>"
        var pre = "<body>hello {{name}}</body>"
        var actual = te.render(pre, mapOf<String, String>("name" to "matt"))

        assertEquals(expected, actual)
    }

    @Test
    fun `should convert a template and list of keys and values into expected html`() {
        var actual = "<body>the {{adjective1}} {{adjective2}} {{noun1}} {{verb}} over the {{adjective3}} {{noun2}}</body>"
        var expected = "<body>the quick brown fox jumped over the lazy dog</body>"

        val mappings = mapOf<String, String>(
            "adjective1" to "quick",
            "adjective2" to "brown",
            "noun1" to "fox",
            "adjective3" to "lazy",
            "noun2" to "dog",
            "verb" to "jumped"
        )
        assertEquals(expected, te.render(actual, mappings))
    }

    @Test
    fun `invalid templating syntax should break the renderer`() {
        var templateMe = "<body>this should {{break}</body>"

        assertThrows(AssertionError::class.java) {te.render(templateMe, mapOf())}
    }
}