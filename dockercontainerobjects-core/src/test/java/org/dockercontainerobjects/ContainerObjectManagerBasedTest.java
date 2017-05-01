package org.dockercontainerobjects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class ContainerObjectManagerBasedTest {

    protected static final int DEFAULT_MAX_SECONDS_TO_WAIT = 30;

    protected static ContainerObjectsEnvironment env;
    protected static ContainerObjectsManager manager;

    @BeforeAll
    static void createManager() {
        env = ContainerObjectsEnvironmentFactory.getInstance().newDefaultEnvironment();
        manager = env.getManager();
    }

    @AfterAll
    static void closeManager() throws Exception {
        env.close();
    }

    protected static <T> void assertWithContainerInstance(Class<T> containerType, Consumer<T> callback) {
        T container = manager.create(containerType);
        assertNotNull(container);
        try {
            assertTrue(manager.isContainerRunning(container));
            callback.accept(container);
        } finally {
            manager.destroy(container);
        }
    }

    protected boolean isUp(Object containerInstance) {
        return manager.isContainerRunning(containerInstance);
    }

    protected void assertContainerRuns(Class<?> containerType) {
        assertWithContainerInstance(containerType, containerInstance -> assertTrue(waitUntilReady(containerInstance)));
    }

    protected boolean waitUntilReady(Object containerInstance, int maxSecondsToWait) {
        try {
            Instant start = Instant.now();
            if (!manager.isContainerRunning(containerInstance)) return false;
            if (isUp(containerInstance)) return true;
            for (int seconds = 1; seconds <= maxSecondsToWait; seconds++) {
                Instant sleepTo = start.plus(Duration.ofSeconds(seconds));
                Duration sleepFor = Duration.between(Instant.now(), sleepTo);
                if (!sleepFor.isNegative())
                    Thread.sleep(sleepFor.toMillis());
                if (isUp(containerInstance)) return true;
            }
            return false;
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected final boolean waitUntilReady(Object containerInstance) {
        return waitUntilReady(containerInstance, DEFAULT_MAX_SECONDS_TO_WAIT);
    }
}
