package coverosR3z.domainobjects

import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.jsonSerialzation
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EmployeeTests {

    @Test
    fun `can serialize Employee with Kotlin serialization`() {
        // serializing objects
        val jsonData = jsonSerialzation.encodeToString(Employee.serializer(), DEFAULT_EMPLOYEE)
        assertEquals("""{"id":{"value":1},"name":{"value":"DefaultEmployee"}}""", jsonData)

        // serializing lists
        val jsonList = jsonSerialzation.encodeToString(ListSerializer(Employee.serializer()), listOf(DEFAULT_EMPLOYEE))
        assertEquals("""[{"id":{"value":1},"name":{"value":"DefaultEmployee"}}]""", jsonList)

        // parsing data back
        val obj: Employee = jsonSerialzation.decodeFromString(Employee.serializer(), """{"id":{"value":1},"name":{"value":"DefaultEmployee"}}""")
        assertEquals(DEFAULT_EMPLOYEE, obj)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong`() {
        val ex = assertThrows(Exception::class.java) {
            jsonSerialzation.decodeFromString(Employee.serializer(), """{"id":{"value":1ABC},"name":{"value":"DefaultEmployee"}}""") }
        assertEquals("Unexpected JSON token at offset 20: Failed to parse 'int'\n" +
                "JSON input: {\"id\":{\"value\":1ABC},\"name\":{\"value\":\"DefaultEmployee\"}}", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, empty name`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            jsonSerialzation.decodeFromString(Employee.serializer(), """{"id":{"value":1},"name":{"value":""}}""") }
        assertEquals("All employees must have a non-empty name", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, empty id`() {
        val ex = assertThrows(Exception::class.java) {
            jsonSerialzation.decodeFromString(Employee.serializer(), """{"id":,"name":{"value":"DefaultEmployee"}}""") }
        assertEquals("Unexpected JSON token at offset 6: Expected '{, kind: CLASS'\n" +
                "JSON input: {\"id\":,\"name\":{\"value\":\"DefaultEmployee\"}}", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, negative id`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            jsonSerialzation.decodeFromString(Employee.serializer(), """{"id":{"value":-1},"name":{"value":"DefaultEmployee"}}""") }
        assertEquals("Valid identifier values are 1 or above", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, too large id`() {
        val ex = assertThrows(Exception::class.java) {
            jsonSerialzation.decodeFromString(Employee.serializer(), """{"id":{"value":12345678901234567890},"name":{"value":"DefaultEmployee"}}""") }
        assertEquals("Unexpected JSON token at offset 36: Failed to parse 'int'\n" +
                "JSON input: {\"id\":{\"value\":12345678901234567890},\"name\":{\"value\":\"DefaultEmployee\"}}", ex.message)
    }
}