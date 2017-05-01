package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.dockercontainerobjects.annotations.BuildImage;
import org.dockercontainerobjects.annotations.EnvironmentEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@DisplayName("Container object building tests")
@Tag("docker")
public class ContainerObjectBuildingIncludingEnvironmentTest extends ContainerObjectManagerBasedTest {

    static final String TEST_DOCKERFILE_PATH = "/ContainerObjectBuildingIncludingEnvironmentTest_Dockerfile";
    static final String TEST_DOCKERFILE_URL = "classpath://"+TEST_DOCKERFILE_PATH;

    static final String TEST_ENV_ENTRY_KEY="TEXT";
    static final String TEST_ENV_ENTRY_VALUE = "this is a test";

    @Test
    @DisplayName("A container object image can define environment variables from annotations on the class")
    void containerContentBuildFromAnnotationOnClass() {
        containerBuildFrom(ContainerContentProvidedByClassAnnotation.class);
    }

    protected <T> void containerBuildFrom(Class<T> containerType) {
        T container = manager.create(containerType);
        assertNotNull(container);
        try {
            assertEquals(ContainerObjectsManager.ContainerStatus.STARTED, manager.getContainerStatus(container));
            String containerAddr = manager.getContainerAddress(container).getHostAddress();
            for (int seconds = 0; !isOK("http://"+containerAddr+":8080/") && seconds < 30; seconds++)
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            assertTrue(isOK("http://"+containerAddr+":8080/sample.txt"));
        } catch (InterruptedException e) {
            fail(e);
        } finally {
            manager.destroy(container);
        }
    }

    protected boolean isOK(String endpoint) {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            return false;
        }
    }

    @BuildImage(TEST_DOCKERFILE_URL)
    @EnvironmentEntry(key=TEST_ENV_ENTRY_KEY, value=TEST_ENV_ENTRY_VALUE)
    public static class ContainerContentProvidedByClassAnnotation {
        // nothing needed
    }
}
