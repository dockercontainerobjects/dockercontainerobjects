package org.dockercontainerobjects.docker.impl.dockerjava

import com.github.dockerjava.core.DockerClientBuilder
import org.dockercontainerobjects.docker.Docker
import org.dockercontainerobjects.docker.ImageName
import org.dockercontainerobjects.docker.ImageSpec
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.nio.file.Files
import java.time.Instant
import java.util.UUID

@TestInstance(Lifecycle.PER_CLASS)
class DockerImageModificationsTest {

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
    fun testSimpleBuildRemove() {
        val IMAGE_NAME_VALUE = "test-simple-"+UUID.randomUUID().toString()
        val IMAGE_NAME = ImageName(IMAGE_NAME_VALUE)

        onTempFolder("DOCKER_") { folder ->
            withEphemeralFile(folder.resolve(DOCKERFILE_FILENAME)) { descriptor ->
                Files.newBufferedWriter(descriptor).use {
                    it.append(DOCKERFILE_CONTENT)
                }
                val spec = ImageSpec(descriptor.toFile()).withTag(IMAGE_NAME)
                docker.images.run {
                    val id = build(spec)
                    assertTrue(isAvailable(id))
                    remove(id)
                    assertFalse(isAvailable(id))
                }
            }
        }
    }

    @Test
    fun testBuildRemoveWithEnvironment() {
        val IMAGE_NAME_VALUE = "test-env-"+UUID.randomUUID().toString()
        val IMAGE_NAME = ImageName(IMAGE_NAME_VALUE)

        val XTRA_LABEL_NAME = "X_BUILD_ON"
        val XTRA_LABEL_VALUE = Instant.now().toString()

        onTempFolder("DOCKER_") { folder ->
            withEphemeralFile(folder.resolve(DOCKERFILE_FILENAME)) { descriptor ->
                Files.newBufferedWriter(descriptor).use {
                    it.append(DOCKERFILE_CONTENT)
                }
                val spec = ImageSpec(descriptor.toFile())
                        .withTag(IMAGE_NAME)
                        .withLabel(XTRA_LABEL_NAME, XTRA_LABEL_VALUE)
                docker.images.run {
                    val id = build(spec)
                    assertTrue(isAvailable(id))
                    val info = inspect(id)
                    assertEquals(XTRA_LABEL_VALUE, info.labels[XTRA_LABEL_NAME])
                    remove(id)
                    assertFalse(isAvailable(id))
                }
            }
        }
    }
}
