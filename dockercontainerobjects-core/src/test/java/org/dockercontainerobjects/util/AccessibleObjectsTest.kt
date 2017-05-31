package org.dockercontainerobjects.util

import org.dockercontainerobjects.util.AccessibleObjects.annotatedWith
import org.dockercontainerobjects.util.AccessibleObjects.isAnnotatedWith
import org.dockercontainerobjects.util.Arrays.stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.lang.reflect.Field
import javax.annotation.Resource

@DisplayName("Accessible objects util tests")
@Tag("util")
class AccessibleObjectsTest {

    @Test
    @DisplayName("isAnnotatedWith can be used to check is a field is annotated")
    fun isAnnotatedWithTest() {
        assertTrue(Sample::class.java.getField("a").isAnnotatedWith(Resource::class))
        assertFalse(Sample::class.java.getField("b").isAnnotatedWith(Resource::class))
    }

    @Test
    @DisplayName("isAnnotatedWith can be used to check is a field is annotated")
    fun annotatedWithTest() {
        assertEquals("a", Sample::class.java.fields.stream().filter(annotatedWith<Field>(Resource::class)).findAny().get().name)
    }

    object Sample {
        @JvmField @Resource var a: String? = null
        @JvmField var b: String? = null
    }
}
