package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("Simple container object tests")
@Tag("docker")
public class SimpleContainerObjectTest extends ContainerObjectManagerBasedTest {

    private static SimpleContainer container;

    @BeforeAll
    static void createContainer() {
        container = manager.create(SimpleContainer.class);
    }

    @AfterAll
    static void destroyContainer() {
        manager.destroy(container);
    }

    @Test
    @DisplayName("A container object, after created, should return status STARTED")
    void containerStatusStarted() {
        assertEquals(ContainerObjectsManager.ContainerStatus.STARTED, manager.getContainerStatus(container));
    }

    @Test
    @DisplayName("A container object, after created, should have non-null id")
    void containerIdNotNull() {
        assertNotNull(manager.getContainerId(container));
    }

    @Test
    @DisplayName("A container object, after created, should have a valid address")
    void containerValidAddress() {
        assertNotNull(manager.getContainerAddress(container));
    }

    @RegistryImage("tomcat:jre8")
    public static class SimpleContainer {
        // nothing needed
    }
}
