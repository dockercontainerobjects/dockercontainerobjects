package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import com.github.dockerjava.api.DockerClient;
import org.dockercontainerobjects.annotations.AfterContainerCreated;
import org.dockercontainerobjects.annotations.AfterContainerStarted;
import org.dockercontainerobjects.annotations.BeforeCreatingContainer;
import org.dockercontainerobjects.annotations.ContainerAddress;
import org.dockercontainerobjects.annotations.ContainerId;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@DisplayName("Container object injection tests")
@Tag("docker")
public class ContainerObjectInjectionTest extends ContainerObjectManagerBasedTest {

    private static InjectedContainer container;

    @BeforeAll
    static void createContainer() {
        container = manager.create(InjectedContainer.class);
    }

    @AfterAll
    static void destroyContainer() {
        manager.destroy(container);
    }

    @Test
    @DisplayName("DockerClient fields are injected before creating a container")
    void dockerClientInjected() {
        assertTrue(container.dockerClientInjected.get());
    }

    @Test
    @DisplayName("ContainerObjectManager fields are injected before creating a container")
    void containerObjectManagerInjected() {
        assertTrue(container.containerObjectManagerInjected.get());
    }

    @Test
    @DisplayName("Container ID fields are injected after creating a container")
    void containerIdInjected() {
        assertTrue(container.containerIdInjected.get());
    }

    @Test
    @DisplayName("Container Address fields are injected after creating a container")
    void containerAddressInjected() {
        assertAll(
                () -> assertTrue(container.containerIPAddressInjected.get()),
                () -> assertTrue(container.containerIPAddressStringInjected.get()));
    }

    @RegistryImage("tomcat:jre8")
    public static class InjectedContainer {

        @Inject
        private DockerClient dockerClient;

        @Inject
        private ContainerObjectsManager containerObjectManager;

        @Inject
        @ContainerId
        private String containerId;

        @Inject
        @ContainerAddress
        private String containerIPAddressString;

        @Inject
        @ContainerAddress
        private InetAddress containerIPAddress;

        public final AtomicBoolean dockerClientInjected = new AtomicBoolean(false);
        public final AtomicBoolean containerObjectManagerInjected = new AtomicBoolean(false);
        public final AtomicBoolean containerIdInjected = new AtomicBoolean(false);
        public final AtomicBoolean containerIPAddressStringInjected = new AtomicBoolean(false);
        public final AtomicBoolean containerIPAddressInjected = new AtomicBoolean(false);

        @BeforeCreatingContainer
        private void beforeCreatingContainer() {
            dockerClientInjected.set(dockerClient != null);
            containerObjectManagerInjected.set(containerObjectManager != null);
        }

        @AfterContainerCreated
        private void afterContainerCreated() {
            containerIdInjected.set(containerId != null);
        }

        @AfterContainerStarted
        private void afterContainerStarted() {
            containerIPAddressInjected.set(containerIPAddress != null);
            containerIPAddressStringInjected.set(containerIPAddressString != null);
        }
    }
}
