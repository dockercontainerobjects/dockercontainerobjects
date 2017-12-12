package org.dockercontainerobjects.docker.impl.dockerjava

import com.github.dockerjava.core.DockerClientBuilder
import org.dockercontainerobjects.docker.Docker
import org.dockercontainerobjects.docker.ImageId
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.time.temporal.ChronoUnit

@TestInstance(Lifecycle.PER_CLASS)
class DockerImagesQueryTest {

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
    fun testId() {
        val id = docker.images.getId(UPSTREAM_IMAGE_NAME)
        assertNotNull(id)
        assertEquals(ImageId.ALGORITHM_SHA256, id.algorithm)
        assertEquals(UPSTREAM_IMAGE_ID, id)
    }

    @Test
    fun testInfo() {
        val info = docker.images.info(UPSTREAM_IMAGE_NAME)
        assertNotNull(info)
        assertEquals(UPSTREAM_IMAGE_ID, info.id)
        assertEquals(UPSTREAM_IMAGE_CREATED, info.created.truncatedTo(ChronoUnit.SECONDS))
        assertEquals(UPSTREAM_IMAGE_SIZE, info.size)
        assertTrue(info.tags.contains(UPSTREAM_IMAGE_NAME))

        val infoByName = docker.images.info(UPSTREAM_IMAGE_NAME)
        val infoById = docker.images.info(UPSTREAM_IMAGE_ID)
        assertEquals(infoByName, infoById)
    }

    @Test
    fun testList() {
        val list = docker.images.list()
        assertNotNull(list)
        val info = list.find { it.tags.contains(UPSTREAM_IMAGE_NAME) }
        assertNotNull(info)
    }

    @Test
    fun testInspect() {
        val info = docker.images.inspect(UPSTREAM_IMAGE_NAME)
        assertNotNull(info)
        assertEquals(UPSTREAM_IMAGE_ID, info.id)
        assertEquals(UPSTREAM_IMAGE_CREATED, info.created.truncatedTo(ChronoUnit.SECONDS))
        assertEquals(UPSTREAM_IMAGE_AUTHOR, info.author)
        assertEquals(UPSTREAM_IMAGE_OS, info.os)
        assertEquals(UPSTREAM_IMAGE_ARCHITECTURE, info.architecture)
        assertEquals(UPSTREAM_IMAGE_SIZE, info.size)
        assertTrue(info.tags.contains(UPSTREAM_IMAGE_NAME))
        assertEquals(ENVREF_VALUE, info.environment[ENVREF_NAME])
        assertTrue(info.labels.isEmpty())

        val infoByName = docker.images.inspect(UPSTREAM_IMAGE_NAME)
        val infoById = docker.images.inspect(UPSTREAM_IMAGE_ID)
        assertEquals(infoByName, infoById)
    }
}
