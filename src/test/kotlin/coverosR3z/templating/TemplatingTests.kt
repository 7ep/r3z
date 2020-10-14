package coverosR3z.templating

import org.junit.Assert.*
import org.junit.Test
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

    @Test
    fun `can read from template file and apply username value`() {
        var toRender = readResourceFile("sample_template.utl")
        var actual = te.render(toRender, mapOf("username" to "Jona"))
        var expected = readResourceFile("sample.html")

        assertEquals(expected, actual)
    }

    @Test
    fun `Should handle bracketed items without anything to map to`() {
        var toRender = "<body>this should {{fail}}</body>"

        var exception = assertThrows(InvalidTemplateException::class.java) {te.render(toRender, mapOf())}
        assertEquals(exception.message, "All double bracketed values must have corresponding mappings")
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    /**
     * Read in template file as a string
     */
    fun readResourceFile(filename: String) : String {
        return javaClass.classLoader.getResource(filename).readBytes().toString(Charsets.UTF_8)
    }
}