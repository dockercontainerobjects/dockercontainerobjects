package org.dockercontainerobjects;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class ContainerObjectManagerBasedTest {

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
}
