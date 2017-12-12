package org.dockercontainerobjects.docker.impl.dockerjava

import com.github.dockerjava.core.DockerClientBuilder
import org.dockercontainerobjects.docker.Docker
import org.dockercontainerobjects.docker.ImageNotFoundException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
class DockerImagesInvalidsTest {

    lateinit var docker: Docker

    @BeforeAll
    fun init() {
        docker = DockerJavaDockerImpl(DockerClientBuilder.getInstance().build())
    }

    @AfterAll
    fun close() {
        docker.close()
    }

    @Test
    fun testIdNoImage() {
        assertThrows<ImageNotFoundException> {
            docker.images.getId(NOIMAGE_NAME)
        }
    }

    @Test
    fun testInfoNoImage() {
        assertThrows<ImageNotFoundException> {
            docker.images.info(NOIMAGE_NAME)
        }
    }

    @Test
    fun testInspectNoImage() {
        assertThrows<ImageNotFoundException> {
            docker.images.inspect(NOIMAGE_NAME)
        }
    }

    @Test
    fun testPullNoImage() {
        assertThrows<ImageNotFoundException> {
            docker.images.pull(NOIMAGE_NAME)
        }
    }
}
