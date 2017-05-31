package org.dockercontainerobjects.resteasy

import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.ContainerObjectsManager
import org.dockercontainerobjects.extensions.BaseContainerObjectsExtension
import org.dockercontainerobjects.util.Fields.ofOneType
import org.jboss.resteasy.client.jaxrs.ResteasyClient
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import java.lang.reflect.Field
import java.net.InetSocketAddress
import java.net.Proxy.Type.DIRECT
import java.net.Proxy.Type.HTTP
import java.util.concurrent.TimeUnit
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder

class ResteasyClientInjectorExtension: BaseContainerObjectsExtension() {

    companion object {
        private val FIELD_SELECTOR = ofOneType(ResteasyClientBuilder::class, ResteasyClient::class, ClientBuilder::class, Client::class)
    }

    override fun <T: Any> getFieldSelectorOnContainerStarted(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldSelectorOnContainerStopped(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldValueOnContainerStarted(ctx: ContainerObjectContext<T>, field: Field): Any {
        val builder = ResteasyClientBuilder()
        val config = field.getAnnotation(ResteasyClientConfig::class.java)
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
        when (proxy.type()) {
            DIRECT -> {}
            HTTP -> {
                val addr = proxy.address() as InetSocketAddress
                builder.defaultProxy(addr.hostString, addr.port, ContainerObjectsManager.SCHEME_HTTP)
            }
            else -> throw IllegalStateException("Unsupported proxy type: ${proxy.type()}")
        }

        val fieldType = field.type
        return when {
            ClientBuilder::class.java.isAssignableFrom(fieldType) -> builder
            Client::class.java.isAssignableFrom(fieldType) -> builder.build()
            else -> throw IllegalStateException()
        }
    }
}
