package org.dockercontainerobjects.junit.runners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import org.dockercontainerobjects.annotations.ContainerObject;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DockerContainerObjectsRunner.class)
public class SimpleClassContainerObjectTest {

    public static final AtomicInteger instanceIdGenerator = new AtomicInteger(0);

    @ContainerObject
    static SimpleContainer container;

    @Test
    public void containerInstantiated1() {
        assertNotNull(container);
        assertEquals(1, container.instanceId);
    }

    @Test
    public void containerInstantiated2() {
        assertNotNull(container);
        assertEquals(1, container.instanceId);
    }

    @RegistryImage("tomcat:jre8")
    public static class SimpleContainer {

        public final int instanceId = instanceIdGenerator.incrementAndGet();
    }
}
