package org.dockercontainerobjects.restassured

import io.restassured.RestAssured
import java.lang.annotation.Inherited

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
@Inherited
annotation class RestAssuredSpecConfig (
    val baseUri: String = RestAssuredInjectorExtension.DEFAULT_BASE_URI,
    val port: Int = RestAssured.DEFAULT_PORT,
    val basePath: String = RestAssured.DEFAULT_PATH,
    val urlEncodingEnabled: Boolean = RestAssured.DEFAULT_URL_ENCODING_ENABLED
)
