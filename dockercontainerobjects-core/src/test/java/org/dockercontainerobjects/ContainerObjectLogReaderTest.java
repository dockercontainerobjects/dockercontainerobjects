package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.fail;

import java.text.ParsePosition;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.dockercontainerobjects.annotations.AfterContainerStopped;
import org.dockercontainerobjects.annotations.BeforeStartingContainer;
import org.dockercontainerobjects.annotations.OnLogEntry;
import org.dockercontainerobjects.annotations.RegistryImage;
import org.dockercontainerobjects.support.ContainerObjectReference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("reading container object log tests")
@Tag("docker")
public class ContainerObjectLogReaderTest extends ContainerObjectManagerBasedTest {

    public static final String SERVER_STARTED_TEMPLATE = "Server startup in \\d+ ms";
    public static final Pattern SERVER_STARTED_PATTERN = Pattern.compile(SERVER_STARTED_TEMPLATE);
    public static final long TIMEOUT_MILLIS = 30000L;

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

    @Test
    @DisplayName("It is possible to read container log after container is restarted")
    @Tag("ISSUE-19")
    @Disabled("Issue if fixed, but this test is brittle. Disabling until updated with a better implementation")
    void readLogAfterRestart() {
        try (ContainerObjectReference<RestartableTomcatContainer> ref = ContainerObjectReference.newReference(env, RestartableTomcatContainer.class)) {
            ref.getInstance().await(TIMEOUT_MILLIS);
            ref.restart();
            ref.getInstance().await(TIMEOUT_MILLIS);
        } catch (RuntimeException ex) {
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

    @RegistryImage("tomcat:jre8")
    public static class RestartableTomcatContainer {

        private volatile CountDownLatch latch;
        private volatile Instant lastMessage;
        private volatile RuntimeException exception;

        @BeforeStartingContainer
        void onStart() {
            latch = new CountDownLatch(1);
            exception = new RuntimeException("log entry never called");
        }

        @AfterContainerStopped
        void onStop() {
            latch = null;
        }

        @OnLogEntry(includeTimestamps = true)
        void onLogEntry(LogEntryContext ctx) {
            exception = null;
            try {
                String line = ctx.getEntryText();
                ParsePosition position = new ParsePosition(0);
                Instant timestamp = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(line, position));
                line = line.substring(position.getIndex()).trim();

                if (lastMessage != null && lastMessage.minus(Duration.ofMillis(200)) .isAfter(timestamp))
                    throw new RuntimeException("Last message received at "+lastMessage+", but new message received with timestamp "+timestamp+" and content: "+line);

                lastMessage = timestamp;
                if (SERVER_STARTED_PATTERN.matcher(line).find()) {
                    latch.countDown();
                    ctx.stop();
                }
            } catch (RuntimeException ex) {
                exception = ex;
                ctx.stop();
            }
        }

        public void await(long timeoutMillis) {
            try {
                if (latch == null)
                    throw new RuntimeException(new NullPointerException());
                boolean completed = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
                if (exception != null)
                    throw exception;
                if (!completed)
                    throw new RuntimeException(new TimeoutException());
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
