package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.dockercontainerobjects.annotations.OnLogEntry;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("reading container object log tests")
@Tag("docker")
public class ContainerObjectLogReaderTest extends ContainerObjectManagerBasedTest {

    public static final String SERVER_STARTED_TEMPLATE = "Server startup in \\d+ ms";
    public static final Pattern SERVER_STARTED_PATTERN = Pattern.compile(SERVER_STARTED_TEMPLATE);
    public static final long TIMEOUT_MILLIS = 5000L;

    @Test
    @DisplayName("It is possible to read container log receiving LogEntryContext instances")
    void readLogUsingEntryContext() {
        try (ContainerObjectReference<LogWithContextTomcatContainer> ref = ContainerObjectReference.newReference(env, LogWithContextTomcatContainer.class)) {
            ref.getInstance().await(TIMEOUT_MILLIS);
        } catch (IllegalStateException ex) {
            fail(ex);
        }
    }

    @Test
    @DisplayName("It is possible to read container log receiving Strings")
    void readLogUsingString() {
        try (ContainerObjectReference<LogWithStringTomcatContainer> ref = ContainerObjectReference.newReference(env, LogWithStringTomcatContainer.class)) {
            ref.getInstance().await(TIMEOUT_MILLIS);
        } catch (IllegalStateException ex) {
            fail(ex);
        }
    }

    @RegistryImage("tomcat:jre8")
    public static class LogWithContextTomcatContainer {

        private final CountDownLatch latch = new CountDownLatch(1);

        @OnLogEntry
        void onLogEntry(LogEntryContext ctx) {
            if (SERVER_STARTED_PATTERN.matcher(ctx.getEntryText()).find()) {
                latch.countDown();
                ctx.stop();
            }
        }

        public void await(long timeoutMillis) {
            try {
                if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS))
                    throw new IllegalStateException();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @RegistryImage("tomcat:jre8")
    public static class LogWithStringTomcatContainer {

        private final CountDownLatch latch = new CountDownLatch(1);

        @OnLogEntry
        void onLogEntry(String line) {
            if (SERVER_STARTED_PATTERN.matcher(line).find())
                latch.countDown();
        }

        public void await(long timeoutMillis) {
            try {
                if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS))
                    throw new IllegalStateException();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
