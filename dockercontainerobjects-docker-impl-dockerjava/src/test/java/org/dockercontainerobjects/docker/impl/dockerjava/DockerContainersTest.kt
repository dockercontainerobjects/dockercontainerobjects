package org.dockercontainerobjects.docker.impl.dockerjava

import com.github.dockerjava.core.DockerClientBuilder
import org.dockercontainerobjects.docker.ContainerName
import org.dockercontainerobjects.docker.ContainerNotFoundException
import org.dockercontainerobjects.docker.ContainerSpec
import org.dockercontainerobjects.docker.ContainerStatus.CREATED
import org.dockercontainerobjects.docker.ContainerStatus.EXITED
import org.dockercontainerobjects.docker.ContainerStatus.PAUSED
import org.dockercontainerobjects.docker.ContainerStatus.RUNNING
import org.dockercontainerobjects.docker.Docker
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.function.Executable
import java.util.UUID

@TestInstance(Lifecycle.PER_CLASS)
class DockerContainersTest {

    lateinit var docker: Docker

    @BeforeAll
    fun init() {
        docker = DockerJavaDockerImpl(DockerClientBuilder.getInstance().build())
        docker.images.pull(UPSTREAM_IMAGE_NAME)
    }

    @AfterAll
    fun close() {
        docker.close()
    }

    @Test
    fun testCreateRemoveWithId() {
        val spec = ContainerSpec(UPSTREAM_IMAGE_ID)
        docker.containers.run {
            val id = create(spec)
            assertEquals(CREATED, status(id))
            remove(id)
        }
    }

    @Test
    fun testCreateRemoveWithName() {
        val spec = ContainerSpec(UPSTREAM_IMAGE_NAME)
        docker.containers.run {
            val id = create(spec)
            assertEquals(CREATED, status(id))
            remove(id)
        }
    }

    @Test
    fun testCreateInspectRemoveWithId() {
        val CONTAINER_NAME_VALUE = "test-simple-"+ UUID.randomUUID().toString()
        val IMAGE_NAME = ContainerName(CONTAINER_NAME_VALUE)
        val XTRA_ENV_NAME = "X_ID"
        val XTRA_ENV_VALUE = UUID.randomUUID().toString()
        val XTRA_LABEL_NAME = "X-ID"
        val XTRA_LABEL_VALUE = UUID.randomUUID().toString()

        val spec = ContainerSpec(UPSTREAM_IMAGE_ID)
                .withName(CONTAINER_NAME_VALUE)
                .withEnvironmentVariable(XTRA_ENV_NAME, XTRA_ENV_VALUE)
                .withLabel(XTRA_LABEL_NAME, XTRA_LABEL_VALUE)

        docker.containers.run {
            val id = create(spec)
            val info = inspect(id)
            assertAll(
                    Executable { assertEquals(CREATED, info.status) },
                    Executable { assertTrue(info.names.contains(IMAGE_NAME)) },
                    Executable { assertEquals(ENVREF_VALUE, info.environment[ENVREF_NAME]) },
                    Executable { assertEquals(XTRA_ENV_VALUE, info.environment[XTRA_ENV_NAME]) },
                    Executable { assertEquals(XTRA_LABEL_VALUE, info.labels[XTRA_LABEL_NAME]) }
            )
            remove(id)
        }
    }

    @Test
    fun testCreateStartPauseResumeStopRemoveWithId() {
        val spec = ContainerSpec(UPSTREAM_IMAGE_NAME)
        docker.containers.run {
            val id = create(spec)
            assertEquals(CREATED, status(id))
            start(id)
            assertEquals(RUNNING, status(id))
            pause(id)
            assertEquals(PAUSED, status(id))
            unpause(id)
            assertEquals(RUNNING, status(id))
            restart(id)
            assertEquals(RUNNING, status(id))
            stop(id)
            assertEquals(EXITED, status(id))
            remove(id)
            assertThrows<ContainerNotFoundException> { status(id) }
        }
    }
}
