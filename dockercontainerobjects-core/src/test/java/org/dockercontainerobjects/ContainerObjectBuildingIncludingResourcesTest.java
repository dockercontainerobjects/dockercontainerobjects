package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.dockercontainerobjects.annotations.BuildImage;
import org.dockercontainerobjects.annotations.BuildImageContent;
import org.dockercontainerobjects.annotations.BuildImageContentEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@DisplayName("Container object building with additional content tests")
@Tag("docker")
public class ContainerObjectBuildingIncludingResourcesTest extends WebContainerObjectManagerBasedTest {

    static final String TEST_DOCKERFILE_PATH = "/ContainerObjectBuildingIncludingResourcesTest_Dockerfile";
    static final String TEST_DOCKERFILE_URL = "classpath://"+TEST_DOCKERFILE_PATH;

    static final String TEST_CONTENT_ENTRY_NAME = "sample.html";
    static final String TEST_CONTENT_ENTRY_URL = "classpath:///"+TEST_CONTENT_ENTRY_NAME;

    @Test
    @DisplayName("A container object image content can be provided from annotations on the class")
    void containerContentBuildFromAnnotationOnClass() {
        containerBuildFrom(ContainerContentProvidedByClassAnnotation.class);
    }

    @Test
    @DisplayName("A container object image content can be provided from a method")
    void containerContenBuildFromAnnotationOnMethod() {
        containerBuildFrom(ContainerContentProvidedByMethod.class);
    }

    protected <T> void containerBuildFrom(Class<T> containerType) {
        assertWithContainerInstance(containerType, containerInstance -> {
            assertTrue(waitUntilReady(containerInstance));
            assertTrue(respondsWithOK(containerInstance, "/sample.html"));
        });
    }

    @BuildImage(TEST_DOCKERFILE_URL)
    @BuildImageContentEntry(name=TEST_CONTENT_ENTRY_NAME, value=TEST_CONTENT_ENTRY_URL)
    public static class ContainerContentProvidedByClassAnnotation {
        // nothing needed
    }

    @BuildImage(TEST_DOCKERFILE_URL)
    public static class ContainerContentProvidedByMethod {

        @BuildImageContent
        Map<String, Object> dockerImageContent() {
            Map<String, Object> content = new HashMap<>();
            content.put(TEST_CONTENT_ENTRY_NAME, TEST_CONTENT_ENTRY_URL);
            return content;
        }
    }
}
