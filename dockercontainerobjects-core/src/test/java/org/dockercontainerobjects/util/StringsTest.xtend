package org.dockercontainerobjects.util

import static extension org.dockercontainerobjects.util.Strings.toCapitalCase
import static extension org.dockercontainerobjects.util.Strings.toSnakeCase
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@RunWith(JUnitPlatform)
@DisplayName("String util tests")
@Tag("util")
class StringsTest {

    @Test
    @DisplayName("given a string formated in capital case, when toCapitalCase, it does not change")
    def void capitalCaseIdentity() {
        assertEquals("Identity", "Identity".toCapitalCase)
    }

    @Test
    @DisplayName("given a string in lower case, when toCapitalCase, it does change to capital case")
    def void capitalCaseFromLower() {
        assertEquals("Lowercase", "lowercase".toCapitalCase)
    }

    @Test
    @DisplayName("given a string in upper case, when toCapitalCase, it does change to capital case")
    def void capitalCaseFromUpper() {
        assertEquals("Uppercase", "UPPERCASE".toCapitalCase)
    }

    @Test
    @DisplayName("given a string in camel case, when toCapitalCase, it does change to capital case")
    def void capitalCaseFromCamel() {
        assertEquals("Camelcase", "CamelCase".toCapitalCase)
    }

    @Test
    @DisplayName("given a string formated in snake case, when toSnakeCase, it does not change")
    def void snakeCaseIdentity() {
        assertEquals("snake_identity", "snake_identity".toSnakeCase)
    }

    @Test
    @DisplayName("given a string in lower case, when toSnakeCase, it does not change")
    def void snakeCaseFromLower() {
        assertEquals("lowercase", "lowercase".toSnakeCase)
    }

    @Test
    @DisplayName("given a string in upper case, when toSnakeCase, it does change to snake case")
    def void snakeCaseFromUpper() {
        assertEquals("uppercase", "UPPERCASE".toSnakeCase)
    }

    @Test
    @DisplayName("given a string in camel case, when toSnakeCase, it does change to snake case")
    def void snakeCaseFromCamel() {
        assertEquals("camel_case", "CamelCase".toSnakeCase)
    }

    @Test
    @DisplayName("given a string in camel case with multiple contiguous capitals, when toSnakeCase, it does change to snake case")
    def void snakeCaseFromCamelContiguousCapitals() {
        assertEquals("url_connection", "URLConnection".toSnakeCase)
    }

    @Test
    @DisplayName("given a string in camel case with multiple contiguous capitals and ending in capital, when toSnakeCase, it does change to snake case")
    def void snakeCaseFromCamelContiguousCapitalsEndsCapital() {
        assertEquals("http_url_conn_x", "HttpURLConnX".toSnakeCase)
    }
}
