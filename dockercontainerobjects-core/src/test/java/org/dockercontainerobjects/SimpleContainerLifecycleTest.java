package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import org.dockercontainerobjects.annotations.AfterContainerCreated;
import org.dockercontainerobjects.annotations.AfterContainerRemoved;
import org.dockercontainerobjects.annotations.AfterContainerStarted;
import org.dockercontainerobjects.annotations.AfterContainerStopped;
import org.dockercontainerobjects.annotations.BeforeCreatingContainer;
import org.dockercontainerobjects.annotations.BeforeRemovingContainer;
import org.dockercontainerobjects.annotations.BeforeStartingContainer;
import org.dockercontainerobjects.annotations.BeforeStoppingContainer;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@DisplayName("Container object lifecycle tests")
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
                    () -> assertEquals(1, container.ixBeforeCreatingContainer.get()),
                    () -> assertEquals(2, container.ixAfterContainerCreated.get()),
                    () -> assertEquals(3, container.ixBeforeStartingContainer.get()),
                    () -> assertEquals(4, container.ixAfterContainerStarted.get()),
                    () -> assertEquals(0, container.ixBeforeStoppingContainer.get()),
                    () -> assertEquals(0, container.ixAfterContainerStopped.get()),
                    () -> assertEquals(0, container.ixBeforeRemovingContainer.get()),
                    () -> assertEquals(0, container.ixAfterContainerRemoved.get()));
        } finally {
            manager.destroy(container);
        }
        assertAll(
                () -> assertEquals(ContainerObjectsManager.ContainerStatus.UNKNOWN,
                        manager.getContainerStatus(container)),
                () -> assertEquals(1, container.ixBeforeCreatingContainer.get()),
                () -> assertEquals(2, container.ixAfterContainerCreated.get()),
                () -> assertEquals(3, container.ixBeforeStartingContainer.get()),
                () -> assertEquals(4, container.ixAfterContainerStarted.get()),
                () -> assertEquals(5, container.ixBeforeStoppingContainer.get()),
                () -> assertEquals(6, container.ixAfterContainerStopped.get()),
                () -> assertEquals(7, container.ixBeforeRemovingContainer.get()),
                () -> assertEquals(8, container.ixAfterContainerRemoved.get()));
    }

    @RegistryImage("tomcat:jre8")
    public static class SimpleContainer {

        private static final AtomicInteger IX = new AtomicInteger();

        public final AtomicInteger ixBeforeCreatingContainer = new AtomicInteger();
        public final AtomicInteger ixAfterContainerCreated = new AtomicInteger();
        public final AtomicInteger ixBeforeStartingContainer = new AtomicInteger();
        public final AtomicInteger ixAfterContainerStarted = new AtomicInteger();
        public final AtomicInteger ixBeforeStoppingContainer = new AtomicInteger();
        public final AtomicInteger ixAfterContainerStopped = new AtomicInteger();
        public final AtomicInteger ixBeforeRemovingContainer = new AtomicInteger();
        public final AtomicInteger ixAfterContainerRemoved = new AtomicInteger();

        private static void ix(final AtomicInteger ref) {
            ref.compareAndSet(0, IX.incrementAndGet());
        }

        @BeforeCreatingContainer
        void beforeCreatingContainer() {
            ix(ixBeforeCreatingContainer);
        }

        @AfterContainerCreated
        void afterContainerCreated() {
            ix(ixAfterContainerCreated);
        }

        @BeforeStartingContainer
        void beforeStartingContainer() {
            ix(ixBeforeStartingContainer);
        }

        @AfterContainerStarted
        void afterContainerStarted() {
            ix(ixAfterContainerStarted);
        }

        @BeforeStoppingContainer
        void beforeStoppingContainer() {
            ix(ixBeforeStoppingContainer);
        }

        @AfterContainerStopped
        void afterContainerStopped() {
            ix(ixAfterContainerStopped);
        }

        @BeforeRemovingContainer
        void beforeRemovingContainer() {
            ix(ixBeforeRemovingContainer);
        }

        @AfterContainerRemoved
        void afterContainerRemoved() {
            ix(ixAfterContainerRemoved);
        }
    }
}
