package org.dockercontainerobjects.restassured;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.regex.Pattern;
import javax.inject.Inject;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.dockercontainerobjects.ContainerObjectsEnvironment;
import org.dockercontainerobjects.ContainerObjectsEnvironmentFactory;
import org.dockercontainerobjects.annotations.BuildImage;
import org.dockercontainerobjects.annotations.BuildImageContentEntry;
import org.dockercontainerobjects.support.ContainerObjectReference;
import org.dockercontainerobjects.support.LogBasedLateInitContainerObject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("Container object using rest-assured tests")
@Tag("docker")
@Tag("bug")
public class ContainerObjectUsingRestAssuredTest {

    static final String TEST_DOCKERFILE_PATH = "/ContainerObjectUsingRestAssuredTest_Dockerfile";
    static final String TEST_DOCKERFILE_URL = "classpath://"+TEST_DOCKERFILE_PATH;

    static final String TEST_CONTENT_ENTRY_NAME = "sample.json";
    static final String TEST_CONTENT_ENTRY_URL = "classpath:///"+TEST_CONTENT_ENTRY_NAME;

    static ContainerObjectsEnvironment environment;

    @BeforeAll
    static void createManager() {
        environment = ContainerObjectsEnvironmentFactory.newEnvironment();
    }

    @AfterAll
    static void closeManager() throws Exception {
        if (environment != null) environment.close();
    }

    @Test
    @DisplayName("A container object with a rest-assured spec injected can test data from container")
    void restAssuredInjectionWorks() {
        try (ContainerObjectReference<RestAssuredContainerObject> ref =
                     ContainerObjectReference.newReference(environment, RestAssuredContainerObject.class)) {
            ref.getInstance().waitForReady();
            RestAssured.given(ref.getInstance().spec)
                    .when()
                        .get("/sample.json")
                    .then()
                        .statusCode(200)
                        .body("message", equalTo("Hello World"));
        }
    }

    @BuildImage(TEST_DOCKERFILE_URL)
    @BuildImageContentEntry(name=TEST_CONTENT_ENTRY_NAME, value=TEST_CONTENT_ENTRY_URL)
    public static class RestAssuredContainerObject extends LogBasedLateInitContainerObject {

        @Inject
        @RestAssuredSpecConfig(port = 8080)
        RequestSpecification spec;

        static final Pattern SERVER_STARTED_PATTERN = Pattern.compile("Server startup in \\d+ ms");

        @Override
        protected int getMaxTimeoutMillis() {
            return 10000;
        }

        @NotNull
        @Override
        protected Pattern getServerReadyLogEntry() {
            return SERVER_STARTED_PATTERN;
        }
    }
}
