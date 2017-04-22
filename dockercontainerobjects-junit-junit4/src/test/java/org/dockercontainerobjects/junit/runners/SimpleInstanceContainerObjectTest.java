package org.dockercontainerobjects.junit.runners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.dockercontainerobjects.annotations.ContainerObject;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DockerContainerObjectsRunner.class)
public class SimpleInstanceContainerObjectTest {

    public static final AtomicInteger instanceIdGenerator = new AtomicInteger(0);

    @ContainerObject
    SimpleContainer container;

    // two test methods implies two containers should be created
    private static AtomicIntegerArray instances = new AtomicIntegerArray(2);

    @Test
    public void containerInstantiated1() {
        assertNotNull(container);
        assertEquals(1, instances.incrementAndGet(container.instanceId));
    }

    @Test
    public void containerInstantiated2() {
        assertNotNull(container);
        assertEquals(1, instances.incrementAndGet(container.instanceId));
    }

    @RegistryImage("tomcat:jre8")
    public static class SimpleContainer {

        public final int instanceId = instanceIdGenerator.getAndIncrement();
    }
}
