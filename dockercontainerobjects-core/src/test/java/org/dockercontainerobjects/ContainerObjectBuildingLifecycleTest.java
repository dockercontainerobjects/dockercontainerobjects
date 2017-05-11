package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.dockercontainerobjects.annotations.AfterContainerCreated;
import org.dockercontainerobjects.annotations.AfterContainerRemoved;
import org.dockercontainerobjects.annotations.AfterContainerStarted;
import org.dockercontainerobjects.annotations.AfterContainerStopped;
import org.dockercontainerobjects.annotations.AfterImageBuilt;
import org.dockercontainerobjects.annotations.AfterImageRemoved;
import org.dockercontainerobjects.annotations.BeforeBuildingImage;
import org.dockercontainerobjects.annotations.BeforeCreatingContainer;
import org.dockercontainerobjects.annotations.BeforeRemovingContainer;
import org.dockercontainerobjects.annotations.BeforeRemovingImage;
import org.dockercontainerobjects.annotations.BeforeStartingContainer;
import org.dockercontainerobjects.annotations.BeforeStoppingContainer;
import org.dockercontainerobjects.annotations.BuildImage;
import org.dockercontainerobjects.annotations.BuildImageContent;
import org.dockercontainerobjects.annotations.Environment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@DisplayName("Container object building lifecycle tests")
@Tag("docker")
public class ContainerObjectBuildingLifecycleTest extends ContainerObjectManagerBasedTest {

    @Test
    @DisplayName("Container follow lifecycle stages")
    void containerLifecycle() {
        SimpleContainer container = manager.create(SimpleContainer.class);
        try {
            assertNotNull(container);
            assertAll(
                    () -> assertEquals(ContainerObjectsManager.ContainerStatus.STARTED,
                            manager.getContainerStatus(container)),
                    () -> assertEquals(1, container.ixBeforeBuildingImage.get()),
                    () -> assertEquals(2, container.ixBuildImage.get()),
                    () -> assertEquals(3, container.ixBuildImageContent.get()),
                    () -> assertEquals(4, container.ixAfterImageBuilt.get()),
                    () -> assertEquals(5, container.ixBeforeCreatingContainer.get()),
                    () -> assertEquals(6, container.ixEnvironment.get()),
                    () -> assertEquals(7, container.ixAfterContainerCreated.get()),
                    () -> assertEquals(8, container.ixBeforeStartingContainer.get()),
                    () -> assertEquals(9, container.ixAfterContainerStarted.get()),
                    () -> assertEquals(0, container.ixBeforeStoppingContainer.get()),
                    () -> assertEquals(0, container.ixAfterContainerStopped.get()),
                    () -> assertEquals(0, container.ixBeforeRemovingContainer.get()),
                    () -> assertEquals(0, container.ixAfterContainerRemoved.get()),
                    () -> assertEquals(0, container.ixBeforeRemovingImage.get()),
                    () -> assertEquals(0, container.ixAfterImageRemoved.get()));
        } finally {
            manager.destroy(container);
        }
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> manager.getContainerStatus(container)),
                () -> assertEquals(1, container.ixBeforeBuildingImage.get()),
                () -> assertEquals(2, container.ixBuildImage.get()),
                () -> assertEquals(3, container.ixBuildImageContent.get()),
                () -> assertEquals(4, container.ixAfterImageBuilt.get()),
                () -> assertEquals(5, container.ixBeforeCreatingContainer.get()),
                () -> assertEquals(6, container.ixEnvironment.get()),
                () -> assertEquals(7, container.ixAfterContainerCreated.get()),
                () -> assertEquals(8, container.ixBeforeStartingContainer.get()),
                () -> assertEquals(9, container.ixAfterContainerStarted.get()),
                () -> assertEquals(10, container.ixBeforeStoppingContainer.get()),
                () -> assertEquals(11, container.ixAfterContainerStopped.get()),
                () -> assertEquals(12, container.ixBeforeRemovingContainer.get()),
                () -> assertEquals(13, container.ixAfterContainerRemoved.get()),
                () -> assertEquals(14, container.ixBeforeRemovingImage.get()),
                () -> assertEquals(15, container.ixAfterImageRemoved.get()));
    }

    public static class SimpleContainer {

        private static final AtomicInteger IX = new AtomicInteger();

        public final AtomicInteger ixBeforeBuildingImage = new AtomicInteger();
        public final AtomicInteger ixBuildImage = new AtomicInteger();
        public final AtomicInteger ixBuildImageContent = new AtomicInteger();
        public final AtomicInteger ixAfterImageBuilt = new AtomicInteger();
        public final AtomicInteger ixBeforeCreatingContainer = new AtomicInteger();
        public final AtomicInteger ixEnvironment = new AtomicInteger();
        public final AtomicInteger ixAfterContainerCreated = new AtomicInteger();
        public final AtomicInteger ixBeforeStartingContainer = new AtomicInteger();
        public final AtomicInteger ixAfterContainerStarted = new AtomicInteger();
        public final AtomicInteger ixBeforeStoppingContainer = new AtomicInteger();
        public final AtomicInteger ixAfterContainerStopped = new AtomicInteger();
        public final AtomicInteger ixBeforeRemovingContainer = new AtomicInteger();
        public final AtomicInteger ixAfterContainerRemoved = new AtomicInteger();
        public final AtomicInteger ixBeforeRemovingImage = new AtomicInteger();
        public final AtomicInteger ixAfterImageRemoved = new AtomicInteger();

        private static void ix(final AtomicInteger ref) {
            ref.compareAndSet(0, IX.incrementAndGet());
        }

        @BeforeBuildingImage
        void beforeBuildingImage() {
            ix(ixBeforeBuildingImage);
        }

        @BuildImage
        String buildImageDockerfile() {
            ix(ixBuildImage);
            return "classpath:///ContainerObjectBuildingLifecycleTest_Dockerfile";
        }

        @BuildImageContent
        Map<String, Object> buildImageContent() {
            ix(ixBuildImageContent);
            return Collections.emptyMap();
        }

        @AfterImageBuilt
        void afterImageBuilt() {
            ix(ixAfterImageBuilt);
        }

        @BeforeCreatingContainer
        void beforeCreatingContainer() {
            ix(ixBeforeCreatingContainer);
        }

        @Environment
        Map<String, String> environmentVariables() {
            ix(ixEnvironment);
            return Collections.emptyMap();
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

        @BeforeRemovingImage
        void beforeRemovingImage() {
            ix(ixBeforeRemovingImage);
        }

        @AfterImageRemoved
        void afterImageRemoved() {
            ix(ixAfterImageRemoved);
        }
    }
}
