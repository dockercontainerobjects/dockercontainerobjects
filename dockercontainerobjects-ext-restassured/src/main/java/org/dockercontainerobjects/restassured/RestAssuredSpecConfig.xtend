package org.dockercontainerobjects.restassured

import java.lang.^annotation.Documented
import java.lang.^annotation.Inherited
import java.lang.^annotation.Retention
import java.lang.^annotation.Target
import io.restassured.RestAssured

@Documented
@Retention(RUNTIME)
@Target(FIELD)
@Inherited
annotation RestAssuredSpecConfig {

    String baseUri = RestAssuredInjectorExtension.DEFAULT_BASE_URI
    int port = RestAssured.DEFAULT_PORT
    String basePath = RestAssured.DEFAULT_PATH
    boolean urlEncodingEnabled = RestAssured.DEFAULT_URL_ENCODING_ENABLED
}
