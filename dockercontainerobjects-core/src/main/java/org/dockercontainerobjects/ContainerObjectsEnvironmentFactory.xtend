package org.dockercontainerobjects

import static extension org.dockercontainerobjects.util.Strings.toCapitalCase

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.Properties
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import org.eclipse.xtend.lib.annotations.Accessors

class ContainerObjectsEnvironmentFactory {

    public static final val PROPERTY_DOCKERNETWORKPROXY_TYPE = "org.dockercontainerobjects.dockernetworkproxy.type";
    public static final val PROPERTY_DOCKERNETWORKPROXY_HOSTNAME = "org.dockercontainerobjects.dockernetworkproxy.hostname";
    public static final val PROPERTY_DOCKERNETWORKPROXY_PORT = "org.dockercontainerobjects.dockernetworkproxy.port";

    public static final val ENV_DOCKERNETWORKPROXY_TYPE = "DOCKER_NETWORKPROXY_TYPE";
    public static final val ENV_DOCKERNETWORKPROXY_HOSTNAME = "DOCKER_NETWORKPROXY_HOSTNAME";
    public static final val ENV_DOCKERNETWORKPROXY_PORT = "DOCKER_NETWORKPROXY_PORT";

    public static final val DEFAULTVALUE_DOCKERNETWORKPROXY_PORT_SOCKS = 1080;
    public static final val DEFAULTVALUE_DOCKERNETWORKPROXY_PORT_HTTP = 8080;

    public static final val PROPERTY_CONTAINERPROXY_TYPE_DIRECT = Proxy.Type.DIRECT.name.toLowerCase

    @Accessors(PUBLIC_GETTER)
    static val instance = new ContainerObjectsEnvironmentFactory

    def newEnvironment(DockerClient dockerClient, Proxy containerProxy) {
        new ContainerObjectsEnvironment(dockerClient, containerProxy)
    }

    def newEnvironment(DockerClient dockerClient) {
        newEnvironment(dockerClient, createDefaultDockerNetworkProxy)
    }

    def newEnvironment(Properties properties) {
        newEnvironment(createDockerClient(properties), createDockerNetworkProxy(properties))
    }

    def newEnvironment() {
        newEnvironment(DockerClientBuilder.instance.build, createDefaultDockerNetworkProxy)
    }

    private def createDockerClient(Properties properties) {
        DockerClientBuilder
            .getInstance(createDockerClientConfig(properties))
            .build
    }

    private def createDockerClientConfig(Properties properties) {
        val builder = DefaultDockerClientConfig.createDefaultConfigBuilder
        for (String propertyName: properties.stringPropertyNames)
            builder.withProperty(propertyName, properties.getProperty(propertyName))
        builder.build
    }

    private def withProperty(DefaultDockerClientConfig.Builder builder, String propertyName, String propertyValue) {
        val methodName = new StringBuilder
        methodName.append("with")
        for (String part: propertyName.split("[_\\.]"))
            methodName.append(part.toCapitalCase)
        try {
            val method = DefaultDockerClientConfig.Builder.getMethod(methodName.toString, String)
            method.invoke(builder, propertyValue)
        } catch (Exception e) {
            // ignore it
        }
    }

    private def createDefaultDockerNetworkProxy() {
        createDockerNetworkProxy(System.properties)
    }

    private def createDockerNetworkProxy(Properties properties) {
        val proxyType = properties.entry(PROPERTY_DOCKERNETWORKPROXY_TYPE, ENV_DOCKERNETWORKPROXY_TYPE, PROPERTY_CONTAINERPROXY_TYPE_DIRECT)
        if (proxyType == PROPERTY_CONTAINERPROXY_TYPE_DIRECT)
            Proxy.NO_PROXY
        else {
            val type = Enum.valueOf(Proxy.Type, proxyType.toUpperCase)
            val host = InetAddress.getByName(properties.entry(PROPERTY_DOCKERNETWORKPROXY_HOSTNAME, ENV_DOCKERNETWORKPROXY_HOSTNAME, null))
            val portValue = properties.entry(PROPERTY_DOCKERNETWORKPROXY_PORT, ENV_DOCKERNETWORKPROXY_PORT, null)
            val port =
                if (portValue !== null && !portValue.empty)
                    Integer.parseInt(portValue)
                else if (type == Proxy.Type.SOCKS)
                    DEFAULTVALUE_DOCKERNETWORKPROXY_PORT_SOCKS
                else
                    DEFAULTVALUE_DOCKERNETWORKPROXY_PORT_HTTP
            new Proxy(type, new InetSocketAddress(host, port))
        }
    }

    private def entry(Properties properties, String propName, String fallbackEnvName, String fallbackValue) {
        properties.getProperty(propName) ?: properties.getProperty(fallbackEnvName) ?: System.getenv(fallbackEnvName) ?: fallbackValue
    }
}
