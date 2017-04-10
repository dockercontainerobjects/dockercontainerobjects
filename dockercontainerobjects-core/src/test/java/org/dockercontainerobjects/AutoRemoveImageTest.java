package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import org.dockercontainerobjects.annotations.RegistryImage;
import org.dockercontainerobjects.docker.DockerClientExtensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@DisplayName("Removable image tests")
@Tag("docker")
public class AutoRemoveImageTest extends ContainerObjectManagerBasedTest {

    public static final String AUTOREMOVABLE_IMAGE_ID = "tomcat:jre7";
    public static final String EXISTING_IMAGE_ID = "tomcat:jre8";

    @Test
    @DisplayName("Images marked as auto-remove should be removed after all containers destroyed")
    void autoremovedImage() {
        assumeFalse(
                DockerClientExtensions.isImageAvailable(env.getDockerClient(), AUTOREMOVABLE_IMAGE_ID),
                "Test requires the image not to exist, or it won't remove it at the end");
        SimpleContainerWithRemovableImage container = manager.create(SimpleContainerWithRemovableImage.class);
        try {
            assertTrue(
                    DockerClientExtensions.isImageAvailable(env.getDockerClient(), AUTOREMOVABLE_IMAGE_ID),
                    "Image must exist after starting first container");
            SimpleContainerWithRemovableImage container2 = manager.create(SimpleContainerWithRemovableImage.class);
            manager.destroy(container2);
            assertTrue(
                    DockerClientExtensions.isImageAvailable(env.getDockerClient(), AUTOREMOVABLE_IMAGE_ID),
                    "Image must still exist after removing first container");
        } finally {
            manager.destroy(container);
        }
        assertFalse(
                DockerClientExtensions.isImageAvailable(env.getDockerClient(), AUTOREMOVABLE_IMAGE_ID),
                "Image must be removed after removing second container");
    }

    @Test
    @DisplayName("Images marked as auto-remove cannot be removed if they exited previously")
    void preexistingImage() {
        DockerClientExtensions.pullImage(env.getDockerClient(), EXISTING_IMAGE_ID);
        SimpleContainerWithExistingImage container = manager.create(SimpleContainerWithExistingImage.class);
        try {
            assertTrue(
                    DockerClientExtensions.isImageAvailable(env.getDockerClient(), EXISTING_IMAGE_ID),
                    "Image must exist after starting first container");
        } finally {
            manager.destroy(container);
        }
        assertTrue(
                DockerClientExtensions.isImageAvailable(env.getDockerClient(), EXISTING_IMAGE_ID),
                "Image must still exist after removing second container");
    }

    @RegistryImage(value = AUTOREMOVABLE_IMAGE_ID, autoRemove = true)
    public static class SimpleContainerWithRemovableImage {
        // nothing needed
    }

    @RegistryImage(value = EXISTING_IMAGE_ID, autoRemove = true)
    public static class SimpleContainerWithExistingImage {
        // nothing needed
    }
}
