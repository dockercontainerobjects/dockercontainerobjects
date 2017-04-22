package org.dockercontainerobjects.junit.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.ClassRule;
import org.junit.Test;

public class SimpleClassRuleContainerObjectTest {

    public static final AtomicInteger instanceIdGenerator = new AtomicInteger(0);

    @ClassRule
    public static ContainerObjectsEnvironmentResource envResource = new ContainerObjectsEnvironmentResource();

    @ClassRule
    public static ContainerObjectResource<SimpleContainer> containerResource =
            new ContainerObjectResource<>(() -> envResource.getManager(), SimpleContainer.class);

    @Test
    public void containerInstantiated1() {
        assertNotNull(containerResource.getContainerInstance());
        assertEquals(1, containerResource.getContainerInstance().instanceId);
    }

    @Test
    public void containerInstantiated2() {
        assertNotNull(containerResource.getContainerInstance());
        assertEquals(1, containerResource.getContainerInstance().instanceId);
    }

    @RegistryImage("tomcat:jre8")
    public static class SimpleContainer {

        public final int instanceId = instanceIdGenerator.incrementAndGet();
    }
}
