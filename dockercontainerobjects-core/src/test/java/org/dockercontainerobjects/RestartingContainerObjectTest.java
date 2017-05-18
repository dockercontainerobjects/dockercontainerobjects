package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.dockercontainerobjects.annotations.AfterContainerRestarted;
import org.dockercontainerobjects.annotations.AfterContainerStopped;
import org.dockercontainerobjects.annotations.BeforeRestartingContainer;
import org.dockercontainerobjects.annotations.BeforeStartingContainer;
import org.dockercontainerobjects.annotations.BeforeStoppingContainer;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@DisplayName("Restarting container object tests")
@Tag("docker")
public class RestartingContainerObjectTest extends ContainerObjectManagerBasedTest {

    @Test
    @DisplayName("A container object can be restarted")
    static void containerCanBeRestarted() {
        RestartableContainer containerInstance = manager.create(RestartableContainer.class);
        try {
            assertEquals(ContainerObjectsManager.ContainerStatus.STARTED, manager.getContainerStatus(containerInstance));
            final String firstContainerId = manager.getContainerId(containerInstance);

            assertAll(
                    () -> assertNotNull(firstContainerId),
                    () -> assertNotNull(manager.getContainerAddress(containerInstance)),
                    () -> assertEquals(0, containerInstance.seq.get()),
                    () -> assertFalse(containerInstance.restarting.get()),
                    () -> assertEquals(0, containerInstance.beforeRestart.get()),
                    () -> assertEquals(0, containerInstance.beforeStop.get()),
                    () -> assertEquals(0, containerInstance.afterStop.get()),
                    () -> assertEquals(0, containerInstance.beforeStart.get()),
                    () -> assertEquals(0, containerInstance.afterStart.get()),
                    () -> assertEquals(0, containerInstance.afterRestart.get())
            );

            manager.restart(containerInstance);
            final String secondContainerId = manager.getContainerId(containerInstance);

            assertAll(
                    () -> assertNotNull(secondContainerId),
                    () -> assertEquals(firstContainerId, secondContainerId),
                    () -> assertNotNull(manager.getContainerAddress(containerInstance)),
                    () -> assertEquals(6, containerInstance.seq.get()),
                    () -> assertFalse(containerInstance.restarting.get()),
                    () -> assertEquals(1, containerInstance.beforeRestart.get()),
                    () -> assertEquals(2, containerInstance.beforeStop.get()),
                    () -> assertEquals(3, containerInstance.afterStop.get()),
                    () -> assertEquals(4, containerInstance.beforeStart.get()),
                    () -> assertEquals(5, containerInstance.afterStart.get()),
                    () -> assertEquals(6, containerInstance.afterRestart.get())
            );
        } finally {
            manager.destroy(containerInstance);
        }
    }

    @RegistryImage("tomcat:jre8")
    public static class RestartableContainer {
        public final AtomicInteger seq = new AtomicInteger();

        public final AtomicBoolean restarting = new AtomicBoolean(false);

        public final AtomicInteger beforeRestart = new AtomicInteger();
        public final AtomicInteger beforeStop = new AtomicInteger();
        public final AtomicInteger afterStop = new AtomicInteger();
        public final AtomicInteger beforeStart = new AtomicInteger();
        public final AtomicInteger afterStart = new AtomicInteger();
        public final AtomicInteger afterRestart = new AtomicInteger();

        @BeforeRestartingContainer
        void beforeRestartingContainer() {
            beforeRestart.compareAndSet(0, seq.incrementAndGet());
            restarting.compareAndSet(false, true);
        }

        @BeforeStoppingContainer
        void beforeStoppingContainer() {
            if (restarting.get())
                beforeStop.compareAndSet(0, seq.incrementAndGet());
        }

        @AfterContainerStopped
        void afterContainerStopped() {
            if (restarting.get())
                afterStop.compareAndSet(0, seq.incrementAndGet());
        }

        @BeforeStartingContainer
        void beforeStartingContainer() {
            if (restarting.get())
                beforeStart.compareAndSet(0, seq.incrementAndGet());
        }

        @AfterContainerStopped
        void afterContainerStarted() {
            if (restarting.get())
                afterStart.compareAndSet(0, seq.incrementAndGet());
        }

        @AfterContainerRestarted
        void afterContainerRestarted() {
            afterRestart.compareAndSet(0, seq.incrementAndGet());
            restarting.compareAndSet(true, false);
        }
    }
}
