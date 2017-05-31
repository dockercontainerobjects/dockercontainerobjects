package org.dockercontainerobjects

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import org.dockercontainerobjects.util.Strings.toCapitalCase
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.Properties

object ContainerObjectsEnvironmentFactory {

    const val PROPERTY_DOCKERNETWORKPROXY_TYPE = "org.dockercontainerobjects.dockernetworkproxy.type"
    const val PROPERTY_DOCKERNETWORKPROXY_HOSTNAME = "org.dockercontainerobjects.dockernetworkproxy.hostname"
    const val PROPERTY_DOCKERNETWORKPROXY_PORT = "org.dockercontainerobjects.dockernetworkproxy.port"

    const val ENV_DOCKERNETWORKPROXY_TYPE = "DOCKER_NETWORKPROXY_TYPE"
    const val ENV_DOCKERNETWORKPROXY_HOSTNAME = "DOCKER_NETWORKPROXY_HOSTNAME"
    const val ENV_DOCKERNETWORKPROXY_PORT = "DOCKER_NETWORKPROXY_PORT"

    const val DEFAULTVALUE_DOCKERNETWORKPROXY_PORT_SOCKS = 1080
    const val DEFAULTVALUE_DOCKERNETWORKPROXY_PORT_HTTP = 8080

    val PROPERTY_CONTAINERPROXY_TYPE_DIRECT = Proxy.Type.DIRECT.name.toLowerCase()

    fun newEnvironment(dockerClient: DockerClient, containerProxy: Proxy = createDockerNetworkProxy()) =
            ContainerObjectsEnvironment(dockerClient, containerProxy)

    fun newEnvironment(properties: Properties) =
        newEnvironment(createDockerClient(properties), createDockerNetworkProxy(properties))

    fun newEnvironment() =
        newEnvironment(DockerClientBuilder.getInstance().build(), createDockerNetworkProxy())

    private inline fun createDockerClient(properties: Properties) =
        DockerClientBuilder
                .getInstance(createDockerClientConfig(properties))
                .build()

    private fun createDockerClientConfig(properties: Properties): DefaultDockerClientConfig {
        val builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
        for (propertyName in properties.stringPropertyNames())
            builder.withProperty(propertyName, properties.getProperty(propertyName))
        return builder.build()
    }

    private fun DefaultDockerClientConfig.Builder.withProperty(propertyName: String, propertyValue: String) {
        val methodName = StringBuilder()
        methodName.append("with")
        for (part in propertyName.split("[_\\.]"))
            methodName.append(part.toCapitalCase())
        try {
            val method = javaClass.getMethod(methodName.toString(), String::class.java)
            method.invoke(this, propertyValue)
        } catch (e: Exception) {
            // ignore it
        }
    }

    private fun createDockerNetworkProxy(properties: Properties = System.getProperties()): Proxy {
        val proxyType = properties.entry(PROPERTY_DOCKERNETWORKPROXY_TYPE, ENV_DOCKERNETWORKPROXY_TYPE, PROPERTY_CONTAINERPROXY_TYPE_DIRECT)
        if (proxyType == PROPERTY_CONTAINERPROXY_TYPE_DIRECT)
            return Proxy.NO_PROXY
        val type = Proxy.Type.valueOf(proxyType.toUpperCase())
        val host = InetAddress.getByName(properties.entry(PROPERTY_DOCKERNETWORKPROXY_HOSTNAME, ENV_DOCKERNETWORKPROXY_HOSTNAME))
        val portValue = properties.entry(PROPERTY_DOCKERNETWORKPROXY_PORT, ENV_DOCKERNETWORKPROXY_PORT)
        val port =
                if (portValue.isNotEmpty())
                    portValue.toInt()
                else if (type == Proxy.Type.SOCKS)
                    DEFAULTVALUE_DOCKERNETWORKPROXY_PORT_SOCKS
                else
                    DEFAULTVALUE_DOCKERNETWORKPROXY_PORT_HTTP
        return Proxy(type, InetSocketAddress(host, port))
    }

    private inline fun Properties.entry(propName: String, fallbackEnvName: String, fallbackValue: String = "") =
        getProperty(propName) ?: getProperty(fallbackEnvName) ?: System.getenv(fallbackEnvName) ?: fallbackValue
}
