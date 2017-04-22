package org.dockercontainerobjects.junit.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class SimpleInstanceRuleContainerObjectTest {

    public static final AtomicInteger instanceIdGenerator = new AtomicInteger(0);

    @ClassRule
    public static ContainerObjectsEnvironmentResource envResource = new ContainerObjectsEnvironmentResource();

    @Rule
    public ContainerObjectResource<SimpleContainer> containerResource =
            new ContainerObjectResource<>(envResource, SimpleContainer.class);

    // two test methods implies two containers should be created
    private static AtomicIntegerArray instances = new AtomicIntegerArray(2);

    @Test
    public void containerInstantiated1() {
        assertNotNull(containerResource.getContainerInstance());
        assertEquals(1, instances.incrementAndGet(containerResource.getContainerInstance().instanceId));
    }

    @Test
    public void containerInstantiated2() {
        assertNotNull(containerResource.getContainerInstance());
        assertEquals(1, instances.incrementAndGet(containerResource.getContainerInstance().instanceId));
    }

    @RegistryImage("tomcat:jre8")
    public static class SimpleContainer {

        public final int instanceId = instanceIdGenerator.getAndIncrement();
    }
}
