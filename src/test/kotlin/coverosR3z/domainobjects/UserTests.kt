package coverosR3z.domainobjects

import coverosR3z.exceptions.MalformedDataDuringSerializationException
import coverosR3z.getResourceAsText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UserTests {

    private val user = User(1, "some user")

    @Test
    fun `can serialize User`() {
        val text = getResourceAsText("/coverosR3z/domainobjects/user_serialized1.txt")
        assertEquals(text, user.serialize())
    }

    @Test
    fun `can deserialize User`() {
        val text = getResourceAsText("/coverosR3z/domainobjects/user_serialized1.txt")
        assertEquals(user, User.deserialize(text))
    }

    /**
     * How is it handled when the number cannot be parsed?
     */
    @Test
    fun `should get exception when deserializing User and see malformed data - bad number`() {
        val errortext_badNumber = getResourceAsText("/coverosR3z/domainobjects/user_serialized_error_bad_number.txt")
        val ex = assertThrows(MalformedDataDuringSerializationException::class.java) { User.deserialize(errortext_badNumber) }
        assertEquals("was unable to deserialize this: ( {id=18438L,name=tester} )", ex.message)
    }

    /**
     * How is it handled when the key value is bad?
     */
    @Test
    fun `should get exception when deserializing User and see malformed data - bad key`() {
        val errortext_badKey = getResourceAsText("/coverosR3z/domainobjects/user_serialized_error_bad_key.txt")
        val ex = assertThrows(MalformedDataDuringSerializationException::class.java) { User.deserialize(errortext_badKey) }
        assertEquals("was unable to deserialize this: ( {foo=18438,name=tester} )", ex.message)
    }

    /**
     * User has two keys - what if we get just one?
     */
    @Test
    fun `should get exception when deserializing User and there's only one key, not two`() {
        val errortext_oneKeyNotTwo = getResourceAsText("/coverosR3z/domainobjects/user_serialized_error_one_key_not_two.txt")
        val ex = assertThrows(MalformedDataDuringSerializationException::class.java) { User.deserialize(errortext_oneKeyNotTwo) }
        assertEquals("was unable to deserialize this: ( {id=18438} )", ex.message)
    }

    /**
     * User has two keys - what if we get three?
     */
    @Test
    fun `should not get exception when deserializing User and there's three keys, not two`() {
        val errortext_ThreeKeysNotTwo = getResourceAsText("/coverosR3z/domainobjects/user_serialized_error_three_keys_not_two.txt")
        val myUser = User.deserialize(errortext_ThreeKeysNotTwo)
        assertEquals(User(id=18438, name="tester,third=third"), myUser)
    }

    /**
     * How is it handled when the data is completely empty?
     */
    @Test
    fun `should get exception when deserializing User and see malformed data - empty`() {
        val errortext_empty = getResourceAsText("/coverosR3z/domainobjects/user_serialized_error_empty.txt")
        val ex = assertThrows(MalformedDataDuringSerializationException::class.java) { User.deserialize(errortext_empty) }
        assertEquals("was unable to deserialize this: (  )", ex.message)
    }

    /**
     * How is it handled when the name field is empty?
     */
    @Test
    fun `should get exception when deserializing User and see malformed data - name field empty`() {
        val errortext_nameFieldEmpty = getResourceAsText("/coverosR3z/domainobjects/user_serialized_name_field_empty.txt")

        val ex = assertThrows(AssertionError::class.java) { User.deserialize(errortext_nameFieldEmpty) }
        assertEquals("All users must have a non-empty name", ex.message)
    }

    /**
     * How is it handled when the id field is empty?
     */
    @Test
    fun `should get exception when deserializing User and see malformed data - id field empty`() {
        val errortext_idFieldEmpty = getResourceAsText("/coverosR3z/domainobjects/user_serialized_id_field_empty.txt")
        val ex = assertThrows(MalformedDataDuringSerializationException::class.java) { User.deserialize(errortext_idFieldEmpty) }
        assertEquals("was unable to deserialize this: ( {id=,name=some user} )", ex.message)
    }

    /**
     * How is it handled when the number is too large? How is it handled when the id is too large? like past 100 million?
     */
    @Test
    fun `should get exception when deserializing User and see malformed data - id too high`() {
        val errortext_tooLargeId = getResourceAsText("/coverosR3z/domainobjects/user_serialized_error_toolargeid.txt")
        val ex = assertThrows(java.lang.AssertionError::class.java) { User.deserialize(errortext_tooLargeId) }
        assertEquals("No way this company has more than 100 million employees", ex.message)
    }

    /**
     * How is it handled when the number is just frankly too large for the number type?
     */
    @Test
    fun `should get exception when deserializing User and see malformed data - too large`() {
        val errortext_tooLarge = getResourceAsText("/coverosR3z/domainobjects/user_serialized_error_toolarge.txt")
        val ex = assertThrows(MalformedDataDuringSerializationException::class.java) { User.deserialize(errortext_tooLarge) }
        assertEquals("was unable to deserialize this: ( {id=123456789012345678901234567890,name=tester} )", ex.message)
    }
}