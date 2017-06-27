package org.dockercontainerobjects.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.stream.Collectors

@DisplayName("Fields util tests")
@Tag("util")
class FieldsTest {

    @Test
    @DisplayName("isOfType can be used to verify the type of a field")
    fun isOfTypeTest() {
        assertTrue(Sample::class.java.getField("b").isOfType<String>())
    }

    @Test
    @DisplayName("isOfOneType can be used to verify the possible types of a field")
    fun isOfOneTypeTest() {
        assertTrue(Sample::class.java.getField("b").isOfOneType(Number::class, String::class))
        assertTrue(Sample::class.java.getField("c").isOfOneType(Number::class, String::class))
    }

    @Test
    @DisplayName("ofType can be used to find fields by its type")
    fun ofTypeTest() {
        val fields = Sample::class.java.fields.stream().filter(ofType<String>()).collect(Collectors.toList())
        assertEquals(1, fields.size)
        val field = fields[0]
        assertEquals("b", field.name)
    }

    @Test
    @DisplayName("ofOneType can be used to find fields by their type")
    fun ofOneTypeTest() {
        val fields = Sample::class.java.fields.stream().filter(ofOneType(String::class, Number::class)).collect(Collectors.toList())
        assertEquals(2, fields.size)
        fields.forEach { assertTrue(it.name == "b" || it.name == "c") }
    }

    object Sample {
        @JvmField var a: Any? = null
        @JvmField var b: String? = null
        @JvmField var c: Number? = null
    }
}
