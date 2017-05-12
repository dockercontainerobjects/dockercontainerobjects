package org.dockercontainerobjects.resteasy

import static org.dockercontainerobjects.util.Fields.ofOneType

import java.lang.reflect.Field
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.extensions.BaseContainerObjectsExtension
import org.jboss.resteasy.client.jaxrs.ResteasyClient
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.dockercontainerobjects.ContainerObjectsManager

class ResteasyClientInjectorExtension extends BaseContainerObjectsExtension {

    private static val FIELD_SELECTOR = ofOneType(ResteasyClientBuilder, ResteasyClient, ClientBuilder, Client)

    override <T> getFieldSelectorOnContainerStarted(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldSelectorOnContainerStopped(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldValueOnContainerStarted(ContainerObjectContext<T> ctx, Field field) {
        val builder = new ResteasyClientBuilder
        val config = field.getAnnotation(ResteasyClientConfig)
        if (config !== null)
            builder
                .hostnameVerification(config.policy)
                .connectionPoolSize(config.connectionPoolSize)
                .maxPooledPerRoute(config.maxPooledPerRoute)
                .connectionTTL(config.connectionTTLMillis, TimeUnit.MILLISECONDS)
                .socketTimeout(config.socketTimeoutMillis, TimeUnit.MILLISECONDS)
                .establishConnectionTimeout(config.establishConnectionTimeoutMillis, TimeUnit.MILLISECONDS)
                .connectionCheckoutTimeout(config.connectionCheckoutTimeoutMillis, TimeUnit.MILLISECONDS)
                .responseBufferSize(config.responseBufferSize)
        val proxy = ctx.environment.dockerNetworkProxy
        switch (proxy.type) {
            case DIRECT: {}
            case HTTP: {
                val addr = proxy.address as InetSocketAddress
                builder.defaultProxy(addr.hostString, addr.port, ContainerObjectsManager.SCHEME_HTTP)
            }
            default:
                throw new IllegalStateException("Unsupported proxy type: "+proxy.type)
        }

        val fieldType = field.type
        if (ClientBuilder.isAssignableFrom(fieldType))
            return builder
        else if (Client.isAssignableFrom(fieldType))
            return builder.build
    }
}
