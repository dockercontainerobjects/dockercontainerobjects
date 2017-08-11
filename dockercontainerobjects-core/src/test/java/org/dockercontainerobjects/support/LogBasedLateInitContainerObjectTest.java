package org.dockercontainerobjects.support;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.dockercontainerobjects.ContainerObjectManagerBasedTest;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("extending LogBasedLateInitContainerObject tests")
@Tag("docker")
@Tag("new")
public class LogBasedLateInitContainerObjectTest extends ContainerObjectManagerBasedTest {

    @Test
    @DisplayName("Simple container")
    void simpleContainer() {
        try (ContainerObjectReference<SimpleContainer> ref = ContainerObjectReference.newReference(env, SimpleContainer.class)) {
            ref.getInstance().waitForReady();
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
    public static class SimpleContainer extends LogBasedLateInitContainerObject {

        static final String SERVER_STARTED_TEMPLATE = "Server startup in \\d+ ms";
        static final Pattern SERVER_STARTED_PATTERN = Pattern.compile(SERVER_STARTED_TEMPLATE);

        @Override
        protected int getMaxTimeoutMillis() {
            return 10000;
        }

        @Override
        protected Pattern getServerReadyLogEntry() {
            return SERVER_STARTED_PATTERN;
        }
    }

    @RegistryImage("tomcat:jre8")
    public static class WrongContainer extends LogBasedLateInitContainerObject {

        static final String SERVER_STARTED_TEMPLATE = "This will never be matched";
        static final Pattern SERVER_STARTED_PATTERN = Pattern.compile(SERVER_STARTED_TEMPLATE);

        @Override
        protected int getMaxTimeoutMillis() {
            return 10000;
        }

        @Override
        protected Pattern getServerReadyLogEntry() {
            return SERVER_STARTED_PATTERN;
        }
    }
}
