package coverosR3z.templating

import coverosR3z.misc.FileReader
import org.junit.Assert.*
import org.junit.Test

class TemplatingTests {

    val te = TemplatingEngine()

    @Test
    fun `should be able to generate html with a variable substituted`() {
        val expected = "<body>hello matt</body>"
        val pre = "<body>hello {{name}}</body>"
        val actual = te.render(pre, mapOf<String, String>("name" to "matt"))

        assertEquals(expected, actual)
    }

    @Test
    fun `should convert a template and list of keys and values into expected html`() {
        val actual = "<body>the {{adjective1}} {{adjective2}} {{noun1}} {{verb}} over the {{adjective3}} {{noun2}}</body>"
        val expected = "<body>the quick brown fox jumped over the lazy dog</body>"

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
    fun `uneven amounts of brackets should throw an exception`() {
        val toRender = "{}}"
        val exception = assertThrows(InvalidTemplateException::class.java) {te.render(toRender, mapOf())}
        assertEquals(exception.message, "Invalid syntax; amount of open and closed '{' brackets must match.")
    }

    @Test
    fun `can read from template file and apply username value`() {
        val toRender = FileReader.readNotNull("sample_template.utl")
        val actual = te.render(toRender, mapOf("username" to "Jona"))
        val expected = FileReader.read("sample.html")

        assertEquals(expected, actual)
    }

    @Test
    fun `Should handle bracketed items without anything to map to`() {
        val toRender = "<body>this should {{fail}}</body>"

        val exception = assertThrows(InvalidTemplateException::class.java) {te.render(toRender, mapOf())}
        assertEquals("Invalid syntax; all double bracketed values must have corresponding mappings.", exception.message)
    }

    @Test
    fun `Should handle multiple values with a template file`() {
        val toRender = FileReader.readNotNull("multiple_values_template.utl")
        val actual = te.render(toRender, mapOf("username" to "Byron", "company" to "Coveros"))
        val expected = FileReader.read("multiple_values.html")

        assertEquals(expected, actual)
    }

}