package org.dockercontainerobjects.restassured

import static extension org.dockercontainerobjects.docker.DockerClientExtensions.inetAddress
import static extension java.lang.String.format

import static org.dockercontainerobjects.util.Fields.ofType

import java.lang.reflect.Field
import java.net.InetSocketAddress
import org.dockercontainerobjects.ContainerObjectContext
import org.dockercontainerobjects.ContainerObjectsManager
import org.dockercontainerobjects.extensions.BaseContainerObjectsExtension
import io.restassured.RestAssured
import io.restassured.specification.ProxySpecification
import io.restassured.specification.RequestSpecification

class RestAssuredInjectorExtension extends BaseContainerObjectsExtension {

    public static val HOST_DYNAMIC_PLACEHOLDER = "*"
    public static val DEFAULT_BASE_URI = "http://*"

    private static val HOSTNAME_DEFAULT_TEMPLATE = "%s://%s:%d"

    private static val FIELD_SELECTOR = ofType(RequestSpecification)

    override <T> getFieldSelectorOnContainerStarted(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldSelectorOnContainerStopped(ContainerObjectContext<T> ctx) {
        FIELD_SELECTOR
    }

    override <T> getFieldValueOnContainerStarted(ContainerObjectContext<T> ctx, Field field) {
        val spec = RestAssured.given
        val config = field.getAnnotation(RestAssuredSpecConfig)
        if (config !== null) {
            if (config.baseUri !== null && config.baseUri != DEFAULT_BASE_URI)
                spec.baseUri(
                        config.baseUri.replace(HOST_DYNAMIC_PLACEHOLDER, ctx.networkSettings.inetAddress.hostAddress))
            else
                spec.baseUri(
                        HOSTNAME_DEFAULT_TEMPLATE.format(
                                ContainerObjectsManager.SCHEME_HTTP,
                                ctx.networkSettings.inetAddress.hostAddress,
                                config.port))
            if (config.basePath !== null && !config.basePath.empty)
                spec.basePath(config.basePath)
            spec.urlEncodingEnabled(config.urlEncodingEnabled)
        }
        val proxy = ctx.environment.dockerNetworkProxy
        switch (proxy.type) {
            case DIRECT: {}
            case HTTP: {
                val addr = proxy.address as InetSocketAddress
                spec.proxy(new ProxySpecification(addr.hostString, addr.port, ContainerObjectsManager.SCHEME_HTTP))
            }
            default:
                throw new IllegalStateException("Unsupported proxy type: "+proxy.type)
        }
        spec
    }
}
