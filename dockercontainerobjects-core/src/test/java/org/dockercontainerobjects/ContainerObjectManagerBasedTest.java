package org.dockercontainerobjects;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class ContainerObjectManagerBasedTest {

    protected static ContainerObjectsManager manager;

    @BeforeAll
    static void createManager() {
        manager = ContainerObjectsEnvironmentFactory.getInstance().newDefaultEnvironment().getManager();
    }

    @AfterAll
    static void closeManager() throws Exception {
        manager.close();
    }
}
