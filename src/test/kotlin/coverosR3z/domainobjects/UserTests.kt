package coverosR3z.domainobjects

import coverosR3z.exceptions.MalformedDataDuringSerializationException
import coverosR3z.getResourceAsText
import coverosR3z.jsonSerialzation
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.JsonDecodingException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.lang.AssertionError

class UserTests {

    private val user = User(1, "some user")

    @Test
    fun `can serialize User with Kotlin serialization`() {
        // serializing objects
        val jsonData = jsonSerialzation.stringify(User.serializer(), user)
        assertEquals("""{"id":1,"name":"some user"}""", jsonData)

        // serializing lists
        val jsonList = jsonSerialzation.stringify(User.serializer().list, listOf(user))
        assertEquals("""[{"id":1,"name":"some user"}]""", jsonList)

        // parsing data back
        val obj: User = jsonSerialzation.parse(User.serializer(), """{"id":1,"name":"some user"}""")
        assertEquals(user, obj)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong`() {
        val ex = assertThrows(JsonDecodingException::class.java) {
            jsonSerialzation.parse(User.serializer(), """{"id":1ABC,"name":"some user"}""") }
        assertEquals("Unexpected JSON token at offset 11: Failed to parse 'int'.\n" +
                " JSON input: {\"id\":1ABC,\"name\":\"some user\"}", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, empty name`() {
        val ex = assertThrows(AssertionError::class.java) {
            jsonSerialzation.parse(User.serializer(), """{"id":1,"name":""}""") }
        assertEquals("All users must have a non-empty name", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, empty id`() {
        val ex = assertThrows(JsonDecodingException::class.java) {
            jsonSerialzation.parse(User.serializer(), """{"id":,"name":"some user"}""") }
        assertEquals("Unexpected JSON token at offset 6: Expected string or non-null literal.\n" +
                " JSON input: {\"id\":,\"name\":\"some user\"}", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, negative id`() {
        val ex = assertThrows(AssertionError::class.java) {
            jsonSerialzation.parse(User.serializer(), """{"id":-1,"name":"some user"}""") }
        assertEquals("Valid identifier values are 1 or above", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, too large id`() {
        val ex = assertThrows(JsonDecodingException::class.java) {
            jsonSerialzation.parse(User.serializer(), """{"id":123456789012345678901234567890,"name":"some user"}""") }
        assertEquals("Unexpected JSON token at offset 37: Failed to parse 'int'.\n" +
                " JSON input: {\"id\":123456789012345678901234567890,\"name\":\"some user\"}", ex.message)
    }
}