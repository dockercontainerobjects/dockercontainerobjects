package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.dockercontainerobjects.annotations.BuildImage;
import org.dockercontainerobjects.annotations.Environment;
import org.dockercontainerobjects.annotations.EnvironmentEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@DisplayName("Container object with envionment tests")
@Tag("docker")
public class ContainerObjectBuildingIncludingEnvironmentTest extends WebContainerObjectManagerBasedTest {

    static final String TEST_DOCKERFILE_PATH = "/ContainerObjectBuildingIncludingEnvironmentTest_Dockerfile";
    static final String TEST_DOCKERFILE_URL = "classpath://"+TEST_DOCKERFILE_PATH;

    static final String TEST_ENV_ENTRY_NAME = "TEXT";
    static final String TEST_ENV_ENTRY_VALUE = "this is a test";

    @Test
    @DisplayName("A container object image can define environment variables from annotations on the class")
    void containerContentBuildFromAnnotationOnClass() {
        containerBuildFrom(ContainerEnvironmentProvidedByClassAnnotation.class);
    }

    @Test
    @DisplayName("A container object image can define environment variables from a method")
    void containerContentBuildFromAnnotationOnMethod() {
        containerBuildFrom(ContainerEnvironmentProvidedByMethod.class);
    }

    protected <T> void containerBuildFrom(Class<T> containerType) {
        assertWithContainerInstance(containerType, containerInstance -> {
            assertTrue(waitUntilReady(containerInstance));
            assertTrue(respondsWithOK(containerInstance, "/sample.txt"));
        });
    }

    @BuildImage(TEST_DOCKERFILE_URL)
    @EnvironmentEntry(name=TEST_ENV_ENTRY_NAME, value=TEST_ENV_ENTRY_VALUE)
    public static class ContainerEnvironmentProvidedByClassAnnotation {
        // nothing needed
    }

    @BuildImage(TEST_DOCKERFILE_URL)
    @EnvironmentEntry(name=TEST_ENV_ENTRY_NAME, value=TEST_ENV_ENTRY_VALUE)
    public static class ContainerEnvironmentProvidedByMethod {

        @Environment
        protected Map<String, String> environment() {
            Map<String, String> env = new HashMap<>();
            env.put(TEST_ENV_ENTRY_NAME, TEST_ENV_ENTRY_VALUE);
            return env;
        }
    }
}
