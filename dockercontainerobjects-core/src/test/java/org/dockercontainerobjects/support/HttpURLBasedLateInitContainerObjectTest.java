package org.dockercontainerobjects.support;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import org.dockercontainerobjects.ContainerObjectManagerBasedTest;
import org.dockercontainerobjects.annotations.BeforeStartingContainer;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.dockercontainerobjects.extensions.URLConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("extending LogBasedLateInitContainerObject tests")
@Tag("docker")
public class HttpURLBasedLateInitContainerObjectTest extends ContainerObjectManagerBasedTest {

    @Test
    @DisplayName("Simple container")
    void simpleContainer() {
        try (ContainerObjectReference<SimpleContainer> ref = ContainerObjectReference.newReference(env, SimpleContainer.class)) {
            ref.getInstance().waitForReady();
            assertTrue(ref.getInstance().beforeReadyCalled.get());
        }
    }

    @Test
    @DisplayName("Wrong container")
    void wrongContainer() {
        try (ContainerObjectReference<WrongContainer> ref = ContainerObjectReference.newReference(env, WrongContainer.class)) {
            ref.getInstance().waitForReady();
            fail("waiting WrongContainer should fail with timeout");
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof TimeoutException);
        }
    }

    @RegistryImage("tomcat:jre8")
    public static class SimpleContainer extends HttpURLBasedLateInitContainerObject {

        @Inject
        @URLConfig(port = 8080)
        protected URL serverUrl;

        @Override
        protected int getMaxTimeoutMillis() {
            return 10000;
        }

        @Override
        protected URL getServerReadyURL() {
            return serverUrl;
        }

        static AtomicBoolean beforeReadyCalled = null;

        @BeforeStartingContainer
        protected void onBeforeStart() {
            beforeReadyCalled = new AtomicBoolean(false);
        }

        @Override
        protected void onBeforeReady() {
            beforeReadyCalled.set(true);
        }
    }

    @RegistryImage("tomcat:jre8")
    public static class WrongContainer extends HttpURLBasedLateInitContainerObject {

        @Inject
        @URLConfig(port = 8000)
        protected URL serverUrl;

        @Override
        protected int getMaxTimeoutMillis() {
            return 10000;
        }

        @Override
        protected URL getServerReadyURL() {
            return serverUrl;
        }
    }
}
