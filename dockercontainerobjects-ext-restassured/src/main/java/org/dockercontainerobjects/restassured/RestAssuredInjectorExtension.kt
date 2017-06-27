package org.dockercontainerobjects.restassured

import io.restassured.RestAssured
import io.restassured.specification.ProxySpecification
import io.restassured.specification.RequestSpecification
import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.ContainerObjectsManager
import org.dockercontainerobjects.docker.DockerClientExtensions.inetAddress
import org.dockercontainerobjects.extensions.BaseContainerObjectsExtension
import org.dockercontainerobjects.util.ofType
import java.lang.reflect.Field
import java.net.InetSocketAddress
import java.net.Proxy.Type.DIRECT
import java.net.Proxy.Type.HTTP

class RestAssuredInjectorExtension: BaseContainerObjectsExtension() {

    companion object {
        const val HOST_DYNAMIC_PLACEHOLDER = "*"
        const val DEFAULT_BASE_URI = "http://*"

        const val HOSTNAME_DEFAULT_TEMPLATE = "%s://%s:%d"
        private val FIELD_SELECTOR = ofType<RequestSpecification>()
    }

    override fun <T: Any> getFieldSelectorOnContainerStarted(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldSelectorOnContainerStopped(ctx: ContainerObjectContext<T>) = FIELD_SELECTOR

    override fun <T: Any> getFieldValueOnContainerStarted(ctx: ContainerObjectContext<T>, field: Field): Any {
        val spec = RestAssured.given()
        val config = field.getAnnotation(RestAssuredSpecConfig::class.java)
        if (config !== null) {
            val hostAddress = ctx.networkSettings?.inetAddress()?.hostAddress ?: throw IllegalStateException()
            if (config.baseUri != DEFAULT_BASE_URI)
                spec.baseUri(
                        config.baseUri.replace(HOST_DYNAMIC_PLACEHOLDER, hostAddress))
            else
                spec.baseUri(
                        HOSTNAME_DEFAULT_TEMPLATE.format(
                                ContainerObjectsManager.SCHEME_HTTP,
                                hostAddress,
                                config.port))
            if (!config.basePath.isNullOrEmpty())
                spec.basePath(config.basePath)
            spec.urlEncodingEnabled(config.urlEncodingEnabled)
        }
        val proxy = ctx.environment.dockerNetworkProxy
        when (proxy.type()) {
            DIRECT -> {}
            HTTP -> {
                val addr = proxy.address() as InetSocketAddress
                spec.proxy(ProxySpecification(addr.hostString, addr.port, ContainerObjectsManager.SCHEME_HTTP))
            }
            else -> throw IllegalStateException("Unsupported proxy type: ${proxy.type()}")
        }
        return spec
    }
}
