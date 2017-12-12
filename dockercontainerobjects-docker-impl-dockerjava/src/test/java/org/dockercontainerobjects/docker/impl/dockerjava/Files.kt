package org.dockercontainerobjects.docker.impl.dockerjava

import java.nio.file.Files
import java.nio.file.Path

fun onTempFolder(prefix: String, task: (Path)->Unit) {
    val folder = Files.createTempDirectory(prefix)
    try {
        task(folder)
    } finally {
        Files.delete(folder)
    }
}

fun withEphemeralFile(file: Path, task: (Path)->Unit) {
    Files.createFile(file)
    try {
        task(file)
    } finally {
        Files.delete(file)
    }
}
