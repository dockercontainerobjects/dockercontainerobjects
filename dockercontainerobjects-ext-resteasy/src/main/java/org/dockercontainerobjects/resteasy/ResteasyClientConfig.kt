package org.dockercontainerobjects.resteasy

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder.HostnameVerificationPolicy
import java.lang.annotation.Inherited

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
@Inherited
annotation class ResteasyClientConfig(
    val policy: HostnameVerificationPolicy = HostnameVerificationPolicy.WILDCARD,
    val connectionPoolSize: Int = 0,
    val maxPooledPerRoute: Int = 0,
    val connectionTTLMillis: Long = -1,
    val socketTimeoutMillis: Long = -1,
    val establishConnectionTimeoutMillis: Long = -1,
    val connectionCheckoutTimeoutMillis: Long = -1,
    val responseBufferSize: Int = 0
)
