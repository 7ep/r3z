package coverosR3z.templating

import coverosR3z.getTime
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

    @Test
    fun `PERFORMANCE - templating`() {
        val numLoops = 10_000
        val maxTime = 1000
        val (time, _) = getTime {
            for(i in 1..numLoops) {
                val toRender = FileReader.readNotNull("multiple_values_template.utl")
                te.render(toRender, mapOf("username" to "Byron", "company" to "Coveros"))
            }
        }

        assertTrue("Should render a $numLoops loops in less than $maxTime millis", time < maxTime)
    }

    /**
     * Use some hand-written code to generate html output
     */
    @Test
    fun testTemplateByCode() {
        val actual = foo("Coveros", "Byron")
        val expected = FileReader.read("multiple_values.html")

        assertEquals(expected, actual)
    }

    fun foo(company: String, username: String) : String {
        return """
<html>
    <head>
    </head>
        <title>demo</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="stylesheet" href="main.css">
    <body>
        <h1>$company</h1>
        <form action="entertime">

            <p>
                Hello there, $username!
            </p>

            <p>
                <label for="project_entry">Project:</label>
                <input id="project_entry" type="text" />
            </p>

            <p>
                <label for="time_entry">Time:</label>
                <input id="time_entry" type="text" />
            </p>

            <p>
                <label for="detail_entry">Details:</label>
                <input id="detail_entry" type="text" />
            </p>

            <p>
                <button>Enter time</button>
            </p>

        </form>
    </body>
</html>
"""
    }

    /**
     * This one results in the same output but uses code
     * instead of a file read with a regular expression text replacement
     */
    @Test
    fun `PERFORMANCE - templating by code`() {
        val numLoops = 100_000
        val maxTime = 1000
        val (time, _) = getTime {
            for(i in 1..numLoops) {
                foo("Coveros", "Byron")
            }
        }

        assertTrue("Should render a $numLoops loops in less than $maxTime millis", time < maxTime)
    }
}