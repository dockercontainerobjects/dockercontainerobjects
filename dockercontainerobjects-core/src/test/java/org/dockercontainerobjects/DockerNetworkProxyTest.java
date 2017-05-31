package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("Docker network proxy tests")
@Tag("util")
public class DockerNetworkProxyTest {

    @Test
    @DisplayName("Environment should have no proxy with clean environment and no properties")
    void noProxy() throws IOException {
        assumeTrue( System.getenv(ContainerObjectsEnvironmentFactory.ENV_DOCKERNETWORKPROXY_TYPE) == null);

        try (ContainerObjectsEnvironment env = ContainerObjectsEnvironmentFactory.INSTANCE.newEnvironment()) {
            assertEquals(Proxy.NO_PROXY, env.getDockerNetworkProxy());
        }
    }

    @Test
    @DisplayName("Environment should have a SOCKS proxy pointing to localhost at port 1080 when SOCKS requested")
    void socksProxy() throws IOException {
        assumeTrue( System.getenv(ContainerObjectsEnvironmentFactory.ENV_DOCKERNETWORKPROXY_HOSTNAME) == null);
        assumeTrue( System.getenv(ContainerObjectsEnvironmentFactory.ENV_DOCKERNETWORKPROXY_PORT) == null);

        Properties properties = new Properties();
        properties.setProperty("org.dockercontainerobjects.dockernetworkproxy.type", "socks");

        try (ContainerObjectsEnvironment env = ContainerObjectsEnvironmentFactory.INSTANCE.newEnvironment(properties)) {
            Proxy proxy = env.getDockerNetworkProxy();
            assertNotNull(proxy);
            assertEquals(Proxy.Type.SOCKS, proxy.type());
            InetSocketAddress addr = (InetSocketAddress) proxy.address();
            assertEquals(1080, addr.getPort());
            assertTrue(addr.getAddress().isLoopbackAddress());
        }
    }

    @Test
    @DisplayName("Environment should have a HTTP proxy pointing to localhost at port 8080 when HTTP requested")
    void httpProxy() throws IOException {
        assumeTrue( System.getenv(ContainerObjectsEnvironmentFactory.ENV_DOCKERNETWORKPROXY_HOSTNAME) == null);
        assumeTrue( System.getenv(ContainerObjectsEnvironmentFactory.ENV_DOCKERNETWORKPROXY_PORT) == null);

        Properties properties = new Properties();
        properties.setProperty("org.dockercontainerobjects.dockernetworkproxy.type", "http");

        try (ContainerObjectsEnvironment env = ContainerObjectsEnvironmentFactory.INSTANCE.newEnvironment(properties)) {
            Proxy proxy = env.getDockerNetworkProxy();
            assertNotNull(proxy);
            assertEquals(Proxy.Type.HTTP, proxy.type());
            InetSocketAddress addr = (InetSocketAddress) proxy.address();
            assertEquals(8080, addr.getPort());
            assertTrue(addr.getAddress().isLoopbackAddress());
        }
    }
}
