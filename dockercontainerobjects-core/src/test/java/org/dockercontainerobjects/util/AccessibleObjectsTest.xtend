package org.dockercontainerobjects.util

import static extension org.dockercontainerobjects.util.AccessibleObjects.isAnnotatedWith

import static org.dockercontainerobjects.util.AccessibleObjects.annotatedWith
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import javax.^annotation.Resource
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform)
@DisplayName("Accessible objects util tests")
@Tag("util")
class AccessibleObjectsTest {

    @Test
    @DisplayName("isAnnotatedWith can be used to check is a field is annotated")
    def void isAnnotatedWithTest() {
        assertTrue(Sample.getField("a").isAnnotatedWith(Resource))
        assertFalse(Sample.getField("b").isAnnotatedWith(Resource))
    }

    @Test
    @DisplayName("isAnnotatedWith can be used to check is a field is annotated")
    def void annotatedWithTest() {
        assertEquals("a", Sample.fields.stream.filter(annotatedWith(Resource)).findAny.get.name)
    }

    static class Sample {
        @Resource public String a
        public String b
    }
}
