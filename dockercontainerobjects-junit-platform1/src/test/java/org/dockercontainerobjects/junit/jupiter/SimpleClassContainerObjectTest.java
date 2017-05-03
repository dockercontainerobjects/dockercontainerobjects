package org.dockercontainerobjects.junit.jupiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import org.dockercontainerobjects.annotations.ContainerObject;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@DisplayName("Simple container object tests (class references)")
@ExtendWith(DockerContainerObjectsExtension.class)
@Tag("docker")
public class SimpleClassContainerObjectTest {

    public static final AtomicInteger instanceIdGenerator = new AtomicInteger(0);

    @ContainerObject
    static SimpleContainer container;

    @Test
    @DisplayName("Container should be instantiated, and only one instanceId")
    void containerInstantiated1() {
        assertNotNull(container);
        assertEquals(1, container.instanceId);
    }

    @Test
    @DisplayName("Container should be instantiated, and only one instanceId")
    void containerInstantiated2() {
        assertNotNull(container);
        assertEquals(1, container.instanceId);
    }

    @RegistryImage("tomcat:jre8")
    public static class SimpleContainer {

        public final int instanceId = instanceIdGenerator.incrementAndGet();
    }
}
