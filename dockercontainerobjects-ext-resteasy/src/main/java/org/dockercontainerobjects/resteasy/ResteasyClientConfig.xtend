package org.dockercontainerobjects.resteasy

import java.lang.^annotation.Documented
import java.lang.^annotation.Inherited
import java.lang.^annotation.Retention
import java.lang.^annotation.Target
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder.HostnameVerificationPolicy

@Documented
@Retention(RUNTIME)
@Target(FIELD)
@Inherited
annotation ResteasyClientConfig {
    HostnameVerificationPolicy policy = HostnameVerificationPolicy.WILDCARD
    int connectionPoolSize = 0
    int maxPooledPerRoute = 0
    long connectionTTLMillis = -1
    long socketTimeoutMillis = -1
    long establishConnectionTimeoutMillis = -1
    int connectionCheckoutTimeoutMillis = -1
    int responseBufferSize = 0
}
