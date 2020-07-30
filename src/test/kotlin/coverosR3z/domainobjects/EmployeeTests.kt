package coverosR3z.domainobjects

import coverosR3z.jsonSerialzation
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.JsonDecodingException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.lang.AssertionError

class EmployeeTests {

    private val employee = Employee(1, "some employee")

    @Test
    fun `can serialize Employee with Kotlin serialization`() {
        // serializing objects
        val jsonData = jsonSerialzation.stringify(Employee.serializer(), employee)
        assertEquals("""{"id":1,"name":"some employee"}""", jsonData)

        // serializing lists
        val jsonList = jsonSerialzation.stringify(Employee.serializer().list, listOf(employee))
        assertEquals("""[{"id":1,"name":"some employee"}]""", jsonList)

        // parsing data back
        val obj: Employee = jsonSerialzation.parse(Employee.serializer(), """{"id":1,"name":"some employee"}""")
        assertEquals(employee, obj)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong`() {
        val ex = assertThrows(JsonDecodingException::class.java) {
            jsonSerialzation.parse(Employee.serializer(), """{"id":1ABC,"name":"some employee"}""") }
        assertEquals("Unexpected JSON token at offset 11: Failed to parse 'int'.\n" +
                " JSON input: {\"id\":1ABC,\"name\":\"some employee\"}", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, empty name`() {
        val ex = assertThrows(AssertionError::class.java) {
            jsonSerialzation.parse(Employee.serializer(), """{"id":1,"name":""}""") }
        assertEquals("All employees must have a non-empty name", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, empty id`() {
        val ex = assertThrows(JsonDecodingException::class.java) {
            jsonSerialzation.parse(Employee.serializer(), """{"id":,"name":"some employee"}""") }
        assertEquals("Unexpected JSON token at offset 6: Expected string or non-null literal.\n" +
                " JSON input: {\"id\":,\"name\":\"some employee\"}", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, negative id`() {
        val ex = assertThrows(AssertionError::class.java) {
            jsonSerialzation.parse(Employee.serializer(), """{"id":-1,"name":"some employee"}""") }
        assertEquals("Valid identifier values are 1 or above", ex.message)
    }

    /**
     * If the persisted data is corrupted, I want this to indicate what
     * was the problem so I can repair it.
     */
    @Test
    fun `failed deserialization should make it clear what went wrong, too large id`() {
        val ex = assertThrows(JsonDecodingException::class.java) {
            jsonSerialzation.parse(Employee.serializer(), """{"id":123456789012345678901234567890,"name":"some employee"}""") }
        assertEquals("Unexpected JSON token at offset 37: Failed to parse 'int'.\n" +
                " JSON input: {\"id\":123456789012345678901234567890,\"name\":\"some employee\"}", ex.message)
    }
}