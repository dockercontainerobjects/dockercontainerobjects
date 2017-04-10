package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import org.dockercontainerobjects.annotations.AfterCreated;
import org.dockercontainerobjects.annotations.AfterRemoved;
import org.dockercontainerobjects.annotations.AfterStarted;
import org.dockercontainerobjects.annotations.AfterStopped;
import org.dockercontainerobjects.annotations.BeforeCreating;
import org.dockercontainerobjects.annotations.BeforeRemoving;
import org.dockercontainerobjects.annotations.BeforeStarting;
import org.dockercontainerobjects.annotations.BeforeStopping;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@DisplayName("Container object dynamic creation tests")
@Tag("docker")
public class SimpleContainerLifecycleTest extends ContainerObjectManagerBasedTest {

    @Test
    @DisplayName("Container follow lifecycle stages")
    void containerLifecycle() {
        SimpleContainer container = manager.create(SimpleContainer.class);
        try {
            assertNotNull(container);
            assertAll(
                    () -> assertEquals(ContainerObjectsManager.ContainerStatus.STARTED,
                            manager.getContainerStatus(container)),
                    () -> assertEquals(1, container.ixBeforeCreating.get()),
                    () -> assertEquals(2, container.ixAfterCreated.get()),
                    () -> assertEquals(3, container.ixBeforeStarting.get()),
                    () -> assertEquals(4, container.ixAfterStarted.get()),
                    () -> assertEquals(0, container.ixBeforeStopping.get()),
                    () -> assertEquals(0, container.ixAfterStopped.get()),
                    () -> assertEquals(0, container.ixBeforeRemoving.get()),
                    () -> assertEquals(0, container.ixAfterRemoved.get()));
        } finally {
            manager.destroy(container);
        }
        assertAll(
                () -> assertEquals(ContainerObjectsManager.ContainerStatus.UNKNOWN,
                        manager.getContainerStatus(container)),
                () -> assertEquals(1, container.ixBeforeCreating.get()),
                () -> assertEquals(2, container.ixAfterCreated.get()),
                () -> assertEquals(3, container.ixBeforeStarting.get()),
                () -> assertEquals(4, container.ixAfterStarted.get()),
                () -> assertEquals(5, container.ixBeforeStopping.get()),
                () -> assertEquals(6, container.ixAfterStopped.get()),
                () -> assertEquals(7, container.ixBeforeRemoving.get()),
                () -> assertEquals(8, container.ixAfterRemoved.get()));
    }

    @RegistryImage("tomcat:jre8")
    public static class SimpleContainer {

        private static final AtomicInteger IX = new AtomicInteger();

        public final AtomicInteger ixBeforeCreating = new AtomicInteger();
        public final AtomicInteger ixAfterCreated = new AtomicInteger();
        public final AtomicInteger ixBeforeStarting = new AtomicInteger();
        public final AtomicInteger ixAfterStarted = new AtomicInteger();
        public final AtomicInteger ixBeforeStopping = new AtomicInteger();
        public final AtomicInteger ixAfterStopped = new AtomicInteger();
        public final AtomicInteger ixBeforeRemoving = new AtomicInteger();
        public final AtomicInteger ixAfterRemoved = new AtomicInteger();

        private static void ix(final AtomicInteger ref) {
            ref.compareAndSet(0, IX.incrementAndGet());
        }

        @BeforeCreating
        void beforeCreating() {
            ix(ixBeforeCreating);
        }

        @AfterCreated
        void afterCreated() {
            ix(ixAfterCreated);
        }

        @BeforeStarting
        void beforeStarting() {
            ix(ixBeforeStarting);
        }

        @AfterStarted
        void afterStarted() {
            ix(ixAfterStarted);
        }

        @BeforeStopping
        void beforeStopping() {
            ix(ixBeforeStopping);
        }

        @AfterStopped
        void afterStopped() {
            ix(ixAfterStopped);
        }

        @BeforeRemoving
        void beforeRemoving() {
            ix(ixBeforeRemoving);
        }

        @AfterRemoved
        void afterRemoved() {
            ix(ixAfterRemoved);
        }
    }
}
