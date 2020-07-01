package com.coveros.r3z.persistence.microorm

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.sql.PreparedStatement

class MicroOrmTests {

    /**
     * A made-up class so we get an exception when used with SqlData
     */
    data class Foo(val a : Int)

    @Test fun `if we pass an unrecognized type to SqlData, we get a good exception message`() {
        // Foo will be unrecognized here, which will cause an exception to be thrown.
        val ex = assertThrows(Exception::class.java) {
            DbAccessHelper.applyParametersToPreparedStatement(mockk(), arrayOf(Foo(1)))
        }
        assertEquals(ex.message, "parameter Foo(a=1) had a type of class com.coveros.r3z.persistence.microorm.MicroOrmTests\$Foo which isn't recognized as a SQL data type.")
    }
}