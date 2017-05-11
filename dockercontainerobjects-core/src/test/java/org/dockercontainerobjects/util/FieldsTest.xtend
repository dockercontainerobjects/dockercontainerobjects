package org.dockercontainerobjects.util

import static extension org.dockercontainerobjects.util.Fields.isOfType
import static extension org.dockercontainerobjects.util.Fields.isOfOneType

import static org.dockercontainerobjects.util.Fields.ofType
import static org.dockercontainerobjects.util.Fields.ofOneType
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.util.stream.Collectors

@RunWith(JUnitPlatform)
@DisplayName("Fields util tests")
@Tag("util")
class FieldsTest {

    @Test
    @DisplayName("isOfType can be used to verify the type of a field")
    def void isOfTypeTest() {
        assertTrue(Sample.getField("b").isOfType(String))
    }

    @Test
    @DisplayName("isOfOneType can be used to verify the possible types of a field")
    def void isOfOneTypeTest() {
        assertTrue(Sample.getField("b").isOfOneType(Number, String))
        assertTrue(Sample.getField("c").isOfOneType(Number, String))
    }

    @Test
    @DisplayName("ofType can be used to find fields by its type")
    def void ofTypeTest() {
        val fields = Sample.fields.stream.filter(ofType(String)).collect(Collectors.toList)
        assertEquals(1, fields.size)
        val field = fields.get(0)
        assertEquals("b", field.name)
    }

    @Test
    @DisplayName("ofOneType can be used to find fields by their type")
    def void ofOneTypeTest() {
        val fields = Sample.fields.stream.filter(ofOneType(String, Number)).collect(Collectors.toList)
        assertEquals(2, fields.size)
        fields.forEach [ assertTrue(name == "b" || name == "c") ]
    }

    static class Sample {
        public Object a
        public String b
        public Number c
    }
}
