package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import org.dockercontainerobjects.annotations.RegistryImage;
import org.dockercontainerobjects.docker.ImageName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("Removable image tests")
@Tag("docker")
public class AutoRemoveImageTest extends ContainerObjectManagerBasedTest {

    static final String AUTOREMOVABLE_IMAGE_ID_VALUE = "tomcat:jre7";
    static final ImageName AUTOREMOVABLE_IMAGE_ID = new ImageName(AUTOREMOVABLE_IMAGE_ID_VALUE);
    static final String EXISTING_IMAGE_ID_VALUE = "tomcat:jre8";
    static final ImageName EXISTING_IMAGE_ID = new ImageName(EXISTING_IMAGE_ID_VALUE);

    @Test
    @DisplayName("Images marked as auto-remove should be removed after all containers destroyed")
    void autoremovedImage() {
        assumeFalse(
                env.getDocker().getImages().isAvailable(AUTOREMOVABLE_IMAGE_ID),
                "Test requires the image not to exist, or it won't remove it at the end");
        SimpleContainerWithRemovableImage container = manager.create(SimpleContainerWithRemovableImage.class);
        try {
            assertTrue(
                    env.getDocker().getImages().isAvailable(AUTOREMOVABLE_IMAGE_ID),
                    "Image must exist after starting first container");
            SimpleContainerWithRemovableImage container2 = manager.create(SimpleContainerWithRemovableImage.class);
            manager.destroy(container2);
            assertTrue(
                    env.getDocker().getImages().isAvailable(AUTOREMOVABLE_IMAGE_ID),
                    "Image must still exist after removing first container");
        } finally {
            manager.destroy(container);
        }
        assertFalse(
                env.getDocker().getImages().isAvailable(AUTOREMOVABLE_IMAGE_ID),
                "Image must be removed after removing second container");
    }

    @Test
    @DisplayName("Images marked as auto-remove cannot be removed if they existed previously")
    void preexistingImage() {
        env.getDocker().getImages().pull(EXISTING_IMAGE_ID);
        SimpleContainerWithExistingImage container = manager.create(SimpleContainerWithExistingImage.class);
        try {
            assertTrue(
                    env.getDocker().getImages().isAvailable(EXISTING_IMAGE_ID),
                    "Image must exist after starting first container");
        } finally {
            manager.destroy(container);
        }
        assertTrue(
                env.getDocker().getImages().isAvailable(EXISTING_IMAGE_ID),
                "Image must still exist after removing second container");
    }

    @RegistryImage(value = AUTOREMOVABLE_IMAGE_ID_VALUE, autoRemove = true)
    public static class SimpleContainerWithRemovableImage {
        // nothing needed
    }

    @RegistryImage(value = EXISTING_IMAGE_ID_VALUE, autoRemove = true)
    public static class SimpleContainerWithExistingImage {
        // nothing needed
    }
}
