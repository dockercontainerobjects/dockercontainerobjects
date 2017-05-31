package org.dockercontainerobjects.util

import org.dockercontainerobjects.util.Strings.toCapitalCase
import org.dockercontainerobjects.util.Strings.toSnakeCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
@DisplayName("String util tests")
@Tag("util")
class StringsTest {

    @Test
    @DisplayName("given a string formated in capital case, when toCapitalCase, it does not change")
    fun capitalCaseIdentity() {
        assertEquals("Identity", "Identity".toCapitalCase())
    }

    @Test
    @DisplayName("given a string in lower case, when toCapitalCase, it does change to capital case")
    fun capitalCaseFromLower() {
        assertEquals("Lowercase", "lowercase".toCapitalCase())
    }

    @Test
    @DisplayName("given a string in upper case, when toCapitalCase, it does change to capital case")
    fun capitalCaseFromUpper() {
        assertEquals("Uppercase", "UPPERCASE".toCapitalCase())
    }

    @Test
    @DisplayName("given a string in camel case, when toCapitalCase, it does change to capital case")
    fun capitalCaseFromCamel() {
        assertEquals("Camelcase", "CamelCase".toCapitalCase())
    }

    @Test
    @DisplayName("given a string formated in snake case, when toSnakeCase, it does not change")
    fun snakeCaseIdentity() {
        assertEquals("snake_identity", "snake_identity".toSnakeCase())
    }

    @Test
    @DisplayName("given a string in lower case, when toSnakeCase, it does not change")
    fun snakeCaseFromLower() {
        assertEquals("lowercase", "lowercase".toSnakeCase())
    }

    @Test
    @DisplayName("given a string in upper case, when toSnakeCase, it does change to snake case")
    fun snakeCaseFromUpper() {
        assertEquals("uppercase", "UPPERCASE".toSnakeCase())
    }

    @Test
    @DisplayName("given a string in camel case, when toSnakeCase, it does change to snake case")
    fun snakeCaseFromCamel() {
        assertEquals("camel_case", "CamelCase".toSnakeCase())
    }

    @Test
    @DisplayName("given a string in camel case with multiple contiguous capitals, when toSnakeCase, it does change to snake case")
    fun snakeCaseFromCamelContiguousCapitals() {
        assertEquals("url_connection", "URLConnection".toSnakeCase())
    }

    @Test
    @DisplayName("given a string in camel case with multiple contiguous capitals and ending in capital, when toSnakeCase, it does change to snake case")
    fun snakeCaseFromCamelContiguousCapitalsEndsCapital() {
        assertEquals("http_url_conn_x", "HttpURLConnX".toSnakeCase())
    }
}
