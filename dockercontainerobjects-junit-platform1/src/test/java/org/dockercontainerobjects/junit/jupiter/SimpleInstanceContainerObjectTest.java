package org.dockercontainerobjects.junit.jupiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.dockercontainerobjects.annotations.ContainerObject;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@DisplayName("Simple container object tests (instanceId references)")
@ExtendWith(DockerContainerObjectsExtension.class)
@Tag("docker")
public class SimpleInstanceContainerObjectTest {

    public static final AtomicInteger instanceIdGenerator = new AtomicInteger(0);

    @ContainerObject
    SimpleContainer container;

    // two test methods implies two containers should be created
    private static AtomicIntegerArray instances = new AtomicIntegerArray(2);

    @Test
    @DisplayName("Container should be instantiated, and multiple instances")
    void containerInstantiated1() {
        assertNotNull(container);
        assertEquals(1, instances.incrementAndGet(container.instanceId));
    }

    @Test
    @DisplayName("Container should be instantiated, and multiple instances")
    void containerInstantiated2() {
        assertNotNull(container);
        assertEquals(1, instances.incrementAndGet(container.instanceId));
    }

    @RegistryImage("tomcat:jre8")
    public static class SimpleContainer {

        public final int instanceId = instanceIdGenerator.getAndIncrement();
    }
}
