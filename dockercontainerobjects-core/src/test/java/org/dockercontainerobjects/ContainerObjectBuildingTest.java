package org.dockercontainerobjects;

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

@DisplayName("Container object building tests")
@Tag("docker")
public class ContainerObjectBuildingTest extends ContainerObjectManagerBasedTest {

    static final String TEST_DOCKERFILE_PATH = "/ContainerObjectBuildingTest_Dockerfile";
    static final String TEST_DOCKERFILE_URL = "classpath://"+TEST_DOCKERFILE_PATH;

    @Test
    @DisplayName("A container object can be built from a BuildImage annotation on the class")
    void containerBuildFromAnnotationOnClass() {
        assertContainerRuns(ContainerBuiltByClassAnnotation.class);
    }

    @Test
    @DisplayName("A container object can be built from a BuildImage annotation on a method returning a URL")
    void containerBuildFromAnnotationOnURLMethod() {
        assertContainerRuns(ContainerBuiltByURLMethod.class);
    }

    @Test
    @DisplayName("A container object can be built from a BuildImage annotation on a method returning an InputStream")
    void containerBuildFromAnnotationOnInputStreamMethod() {
        assertContainerRuns(ContainerBuiltByInputStreamMethod.class);
    }

    @Test
    @DisplayName("A container object can be built from a BuildImage annotation on a method returning a String")
    void containerBuildFromAnnotationOnStringMethod() {
        assertContainerRuns(ContainerBuiltByStringMethod.class);
    }

    @Test
    @DisplayName("A container object can be built from a BuildImage annotation on a method returning a File")
    void containerBuildFromAnnotationOnFileMethod() {
        assertContainerRuns(ContainerBuiltByFileMethod.class);
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
