package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.dockercontainerobjects.annotations.BuildImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@DisplayName("Container object building tests")
@Tag("docker")
public class ContainerObjectBuildingTest extends ContainerObjectManagerBasedTest {

    static final String TEST_DOCKERFILE_PATH = "/ContainerObjectBuildingTest_Dockerfile";
    static final String TEST_DOCKERFILE_URL = "classpath://"+TEST_DOCKERFILE_PATH;

    @Test
    @DisplayName("A container object can be built from a BuildImage annotation on the class")
    void containerBuildFromAnnotationOnClass() {
        containerBuildFrom(ContainerBuiltByClassAnnotation.class);
    }

    @Test
    @DisplayName("A container object can be built from a BuildImage annotation on a method returning a URL")
    void containerBuildFromAnnotationOnURLMethod() {
        containerBuildFrom(ContainerBuiltByURLMethod.class);
    }

    @Test
    @DisplayName("A container object can be built from a BuildImage annotation on a method returning an InputStream")
    void containerBuildFromAnnotationOnInputStreamMethod() {
        containerBuildFrom(ContainerBuiltByInputStreamMethod.class);
    }

    @Test
    @DisplayName("A container object can be built from a BuildImage annotation on a method returning a String")
    void containerBuildFromAnnotationOnStringMethod() {
        containerBuildFrom(ContainerBuiltByStringMethod.class);
    }

    @Test
    @DisplayName("A container object can be built from a BuildImage annotation on a method returning a File")
    void containerBuildFromAnnotationOnFileMethod() {
        containerBuildFrom(ContainerBuiltByFileMethod.class);
    }

    protected <T> void containerBuildFrom(Class<T> containerType) {
        T container = manager.create(containerType);
        assertNotNull(container);
        try {
            assertEquals(ContainerObjectsManager.ContainerStatus.STARTED, manager.getContainerStatus(container));
        } finally {
            manager.destroy(container);
        }
    }

    @BuildImage(TEST_DOCKERFILE_URL)
    public static class ContainerBuiltByClassAnnotation {
        // nothing needed
    }

    public static class ContainerBuiltByStringMethod {

        @BuildImage
        protected String build() {
            return TEST_DOCKERFILE_URL;
        }
    }

    public static class ContainerBuiltByURLMethod {

        @BuildImage
        protected URL build() {
            return getClass().getResource(TEST_DOCKERFILE_PATH);
        }
    }

    public static class ContainerBuiltByInputStreamMethod {

        @BuildImage
        protected InputStream build() {
            return getClass().getResourceAsStream(TEST_DOCKERFILE_PATH);
        }
    }

    public static class ContainerBuiltByFileMethod {

        @BuildImage
        protected File build() {
            File dir = Files.createTempDir();
            File dockerfile = new File(dir, "Dockerfile");
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dockerfile))) {
                Resources.copy(getClass().getResource(TEST_DOCKERFILE_PATH), out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            dockerfile.deleteOnExit();
            dir.deleteOnExit();
            return dir;
        }
    }
}
