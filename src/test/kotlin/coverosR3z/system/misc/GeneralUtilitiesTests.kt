package coverosR3z.system.misc

import coverosR3z.system.misc.utility.checkParseToDouble
import coverosR3z.system.misc.utility.checkParseToInt
import coverosR3z.system.misc.utility.checkParseToLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeneralUtilitiesTests {

    @Test
    fun testParseInteger_NegativeCase_null() {
        val ex = assertThrows(IllegalStateException::class.java){ checkParseToInt(null) }
        assertEquals("Integer must not be a null value", ex.message)
    }

    @Test
    fun testParseInteger_NegativeCase_emptyString() {
        val ex = assertThrows(IllegalStateException::class.java){ checkParseToInt("") }
        assertEquals("Integer must not be blank", ex.message)
    }

    @Test
    fun testParseInteger_NegativeCase_nonNumeric() {
        val ex = assertThrows(IllegalStateException::class.java){ checkParseToInt("abc") }
        assertEquals("""Must be able to parse "abc" as an integer""", ex.message)
    }

    @Test
    fun testParseLong_NegativeCase_nonNumeric() {
        val ex = assertThrows(IllegalStateException::class.java){ checkParseToLong("abc") }
        assertEquals("""Must be able to parse "abc" as a long""", ex.message)
    }

    @Test
    fun testParseDouble_NegativeCase_nonNumeric() {
        val ex = assertThrows(IllegalStateException::class.java){ checkParseToDouble("abc") }
        assertEquals("""Must be able to parse "abc" as a double""", ex.message)
    }

    @Test
    fun testTitleCase_basic() {
        val result: String = "foo foo fah".toTitleCase()
        assertEquals("Foo Foo Fah", result)
    }

    @Test
    fun testTitleCase_moreAdvanced() {
        val result: String = "the Foo foo to The an apple fah".toTitleCase()
        assertEquals("The Foo Foo to the an Apple Fah", result)
    }

    @Test
    fun testTitleCase_edgeCase_EmptyString() {
        val result = assertThrows(IllegalArgumentException::class.java) { "".toTitleCase() }
        assertEquals("cannot title-case an empty string" , result.message)
    }

    @Test
    fun testTitleCase_edgeCase_Number() {
        val result = "1".toTitleCase()
        assertEquals("1", result)
    }

    /**
     * We don't really case about double spacing, currently,
     * it just reduces them to single space
     */
    @Test
    fun testTitleCase_edgeCase_DoubleSpaces() {
        val result = "a  b".toTitleCase()
        assertEquals("A  B", result)
    }

    /**
     * We don't really care about periods.
     */
    @Test
    fun testTitleCase_edgeCase_AfterAPeriod() {
        val result = "a.  an".toTitleCase()
        assertEquals("A.  an", result)
    }

    /**
     * handling leading spaces...
     */
    @Test
    fun testTitleCase_edgeCase_leadingSpace() {
        val result = "   a man a plan a canal panama".toTitleCase()
        assertEquals("   a Man a Plan a Canal Panama", result)
    }

    /**
     * handling different kinds of whitespace
     * (This method is not robust enough to handle
     * whitespace characters mixed into alphabetical characters,
     * but feel free to expand functionality if bored later)
     */
    @Test
    fun testTitleCase_edgeCase_tabs() {
        val result = "a  \tb".toTitleCase()
        assertEquals("A  \tb", result)
    }
}

private fun String.toTitleCase(): String {
    require(this.isNotBlank()){"cannot title-case an empty string"}
    val articles = listOf("a", "an", "the", "to")
    val result = mutableListOf<String>()
    val arr = this.split(" ")
    result.add(
        if (arr[0].isEmpty()) {
            arr[0]
        } else {
            arr[0][0].toUpperCase() + arr[0].substring(1)
        }
    )

    for (i in 1.rangeTo(arr.size-1)) {
        if(arr[i].isEmpty()){
            result.add(arr[i])
        } else if (arr[i].toLowerCase() !in articles) {
            result.add(arr[i][0].toUpperCase() + arr[i].substring(1))
        } else {
            result.add(arr[i][0].toLowerCase() + arr[i].substring(1))
        }
    }
        return result.joinToString(" ")
}
