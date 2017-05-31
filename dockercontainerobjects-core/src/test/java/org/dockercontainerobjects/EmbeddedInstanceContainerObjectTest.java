package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.dockercontainerobjects.annotations.BeforeStartingContainer;
import org.dockercontainerobjects.annotations.ContainerObject;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("Embedded containers object tests")
@Tag("docker")
public class EmbeddedInstanceContainerObjectTest extends ContainerObjectManagerBasedTest {

    public static final AtomicInteger instanceIdGenerator = new AtomicInteger(0);
    public static final AtomicInteger lifecycleIdGenerator = new AtomicInteger(0);

    private static OuterContainer outer;

    @BeforeAll
    static void createContainer() {
        outer = manager.create(OuterContainer.class);
    }

    @AfterAll
    static void destroyContainer() {
        manager.destroy(outer);
    }

    @Test
    @DisplayName("Containers should be instantiated, outer first, then inner")
    void containerInstantiatedInOrder() {
        // both containers where instantiated
        assertNotNull(outer);
        assertNotNull(outer.inner);

        // inner container was instantiated after outer container
        assertTrue(outer.inner.instanceId > outer.instanceId);
    }

    @Test
    @DisplayName("Containers lifecycle should be called, inner first, then outer")
    void containerLifecycleInOrder() {
        // given that both containers where instantiated
        assumeTrue(outer != null);
        assumeTrue(outer.inner != null);

        // then inner container life-cycle called before
        assertTrue(outer.beforeStarting.get() != 0);
        assertTrue(outer.inner.beforeStarting.get() != 0);
        assertTrue(outer.inner.beforeStarting.get() < outer.beforeStarting.get());
    }

    @RegistryImage("tomcat:jre8")
    public static class OuterContainer {

        public final int instanceId = instanceIdGenerator.getAndIncrement();

        public final AtomicInteger beforeStarting = new AtomicInteger();

        @ContainerObject
        InnerContainer inner;

        @BeforeStartingContainer
        private void beforeStartingContainer() {
            beforeStarting.compareAndSet(0, lifecycleIdGenerator.incrementAndGet());
        }
    }

    @RegistryImage("tomcat:jre8")
    public static class InnerContainer {

        public final int instanceId = instanceIdGenerator.getAndIncrement();

        public final AtomicInteger beforeStarting = new AtomicInteger();

        @BeforeStartingContainer
        private void beforeStartingContainer() {
            beforeStarting.compareAndSet(0, lifecycleIdGenerator.incrementAndGet());
        }
    }
}
