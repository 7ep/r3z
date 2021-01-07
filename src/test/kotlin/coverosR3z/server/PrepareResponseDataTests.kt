package coverosR3z.server

import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.StatusCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PrepareResponseDataTests {

    @Test
    fun testEquality() {
        val data1 = PreparedResponseData("", StatusCode.OK, emptyList())
        val data2 = PreparedResponseData("", StatusCode.OK, emptyList())
        assertEquals(data1, data2)
    }

    @Test
    fun testEqualityHash() {
        val data1 = PreparedResponseData("", StatusCode.OK, emptyList())
        val data2 = PreparedResponseData("", StatusCode.OK, emptyList())
        assertEquals(data1.hashCode(), data2.hashCode())
    }

    @Test
    fun testUnequal() {
        val data1 = PreparedResponseData("", StatusCode.OK, emptyList())
        val data2 = PreparedResponseData("", StatusCode.INTERNAL_SERVER_ERROR, emptyList())
        assertNotEquals(data1, data2)
    }

}