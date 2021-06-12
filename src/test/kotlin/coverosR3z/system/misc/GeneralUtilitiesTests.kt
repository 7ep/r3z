package coverosR3z.system.misc

import coverosR3z.system.misc.utility.checkParseToDouble
import coverosR3z.system.misc.utility.checkParseToInt
import coverosR3z.system.misc.utility.checkParseToLong
import coverosR3z.system.misc.utility.toTitleCase
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

    /**
     * The basic happy path for title casing
     */
    @Test
    fun testTitleCase_basic() {
        val result: String = "foo foo fah".toTitleCase()
        assertEquals("Foo Foo Fah", result)
    }

    /**
     * A bit more advanced a test, including articles and
     * needing to lower-case some words
     */
    @Test
    fun testTitleCase_moreAdvanced() {
        val result: String = "the Foo foo to The an apple fah".toTitleCase()
        assertEquals("The Foo Foo to the an Apple Fah", result)
    }

    /**
     * What if we pass in an empty string
     */
    @Test
    fun testTitleCase_edgeCase_EmptyString() {
        val result = assertThrows(IllegalArgumentException::class.java) { "".toTitleCase() }
        assertEquals("cannot title-case an empty string" , result.message)
    }

    /**
     * What if we pass in whitespace - one space
     */
    @Test
    fun testTitleCase_edgeCase_Whitespace_Space() {
        val result = assertThrows(IllegalArgumentException::class.java) { " ".toTitleCase() }
        assertEquals("cannot title-case an empty string" , result.message)
    }

    /**
     * What if we pass in whitespace - two spaces
     */
    @Test
    fun testTitleCase_edgeCase_Whitespace_TwoSpace() {
        val result = assertThrows(IllegalArgumentException::class.java) { "  ".toTitleCase() }
        assertEquals("cannot title-case an empty string" , result.message)
    }

    /**
     * What if we pass in whitespace - tab
     */
    @Test
    fun testTitleCase_edgeCase_Whitespace_Tab() {
        val result = assertThrows(IllegalArgumentException::class.java) { "\t".toTitleCase() }
        assertEquals("cannot title-case an empty string" , result.message)
    }

    /**
     * What if we pass in whitespace - newline
     */
    @Test
    fun testTitleCase_edgeCase_Whitespace_Newline() {
        val result = assertThrows(IllegalArgumentException::class.java) { "\n".toTitleCase() }
        assertEquals("cannot title-case an empty string" , result.message)
    }

    /**
     * What if we just pass in a single number
     */
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

    /**
     * What if we pass in just a single letter
     */
    @Test
    fun testTitleCase_singleLetter() {
        val result = "a".toTitleCase()
        assertEquals("A", result)
    }

    /**
     * What if we pass in just a single word
     */
    @Test
    fun testTitleCase_singleWord() {
        val result = "aaaa".toTitleCase()
        assertEquals("Aaaa", result)
    }

    /**
     * What if we pass in just a single word that is an article ("an")
     */
    @Test
    fun testTitleCase_singleWordArticle() {
        val result = "an".toTitleCase()
        assertEquals("An", result)
    }

    /**
     * What if we pass in just a single word that is an article ("an")
     */
    @Test
    fun testTitleCase_symbol() {
        val result = "!".toTitleCase()
        assertEquals("!", result)
    }

}