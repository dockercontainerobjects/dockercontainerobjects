@file:JvmName("SpecsSupport")

package org.dockercontainerobjects

import org.dockercontainerobjects.annotations.BuildImage
import org.dockercontainerobjects.annotations.OnLogEntry
import org.dockercontainerobjects.annotations.RegistryImage
import org.dockercontainerobjects.docker.ContainerLogEntryContext
import org.dockercontainerobjects.docker.ContainerLogSpec
import org.dockercontainerobjects.docker.ContainerSpec
import org.dockercontainerobjects.docker.ImageName
import org.dockercontainerobjects.util.call
import org.dockercontainerobjects.util.getAnnotation
import org.dockercontainerobjects.util.isOfReturnType
import java.io.File
import java.io.InputStream
import java.lang.reflect.Method
import java.net.URI
import java.net.URL
import java.time.Instant

data class ContainerConfiguration(
        val spec: ContainerSpec,
        val forcePull: Boolean,
        var autoRemove: Boolean
)

fun containerConfigFromClass(
        annotation: RegistryImage,
        containerType: Class<*>
): ContainerConfiguration {
    val image =
            annotation.value.let {
                if (it.isNotEmpty()) {
                    ImageName(it)
                } else {
                    throw IllegalArgumentException(
                            "Annotation '${RegistryImage::class.java.simpleName}' on class '${containerType.simpleName}' must define a value to be used to define the image to use"
                    )
                }
            }
    return ContainerConfiguration(
            spec = ContainerSpec(image),
            forcePull = annotation.forcePull,
            autoRemove = annotation.autoRemove
    )
}

fun <T> containerConfigFromMethod(
        method: Method,
        containerType: Class<T>,
        containerInstance: T
): ContainerConfiguration {
    if (!method.isOfReturnType<String>()) {
        throw IllegalArgumentException(
                "Method '$method' on class '${containerType.simpleName}' must return '${String::class.java.simpleName}' to be used to define the image to use"
        )
    }
    val annotation = method.getAnnotation<RegistryImage>()!!
    if (annotation.value.isNotEmpty()) {
        throw IllegalArgumentException(
                "Annotation '${RegistryImage::class.java.simpleName}' on method '$method' on class '${containerType.simpleName}' cannot define a value to be used to define the image to use"
        )
    }
    val image = (method.call(containerInstance) as String?).let {
        if (it != null && it.isNotEmpty()) {
            ImageName(it)
        } else {
            throw IllegalArgumentException(
                    "Method '$method' on class '${containerType.simpleName}' must return a non-null value to be used to define the image to use"
            )
        }
    }
    return ContainerConfiguration(
            spec = ContainerSpec(image),
            forcePull = annotation.forcePull,
            autoRemove = annotation.autoRemove
    )
}

data class ImageConfiguration(
        var ref: Any,
        var tag: String,
        val forcePull: Boolean
)

fun imageConfigFromClass(
        annotation: BuildImage,
        containerType: Class<*>
): ImageConfiguration {
    val ref =
            annotation.value.let {
                if (it.isNotEmpty()) {
                    it
                } else {
                    throw IllegalArgumentException(
                            "Annotation '${BuildImage::class.java.simpleName}' on class '${containerType.simpleName}' must define a non-empty value pointing to a Dockerfile"
                    )
                }
            }
    return ImageConfiguration(ref, annotation.tag, annotation.forcePull)
}

val IMAGE_CONFIG_METHOD_RETURN_TYPES =
        listOf(String::class, URI::class, URL::class, File::class, InputStream::class)

fun <T> imageConfigFromMethod(
        method: Method,
        containerType: Class<T>,
        containerInstance: T
): ImageConfiguration {
    IMAGE_CONFIG_METHOD_RETURN_TYPES.stream()
            .filter { method.isOfReturnType(it) }
            .findAny()
            .orElseThrow {
                IllegalArgumentException(
                        "Method '$method' on class '${containerType.simpleName}' must return a valid type pointing to a Dockerfile"
                )
            }
    val ref = method.call(containerInstance).let {
        if (it != null) {
            it
        } else {
            throw IllegalArgumentException(
                    "Method '$method' on class '${containerType.simpleName}' must return a non-null value to be used to define the image to use"
            )
        }
    }
    val annotation = method.getAnnotation<BuildImage>()!!
    return ImageConfiguration(ref, annotation.tag, annotation.forcePull)
}

fun <T> containerLogSpecFromMethod(
        method: Method,
        containerType: Class<T>,
        containerInstance: T,
        since: Instant
): ContainerLogSpec {
    if (method.parameterCount != 1) {
        throw IllegalArgumentException(
                "Method '$method' on class '${containerType.simpleName}' must expect a single param of valid type to receive log entries"
        )
    }
    val annotation = method.getAnnotation<OnLogEntry>()!!
    val spec = ContainerLogSpec(
            since = since,
            standartOutputIncluded = annotation.includeStdOut,
            standarErrorIncluded = annotation.includeStdErr,
            timestampsIncluded = annotation.includeTimestamps
    )
    val param = method.parameters.first()
    when (param.type.kotlin) {
        String::class -> spec.onLogEntry {
            method.call(containerInstance, it.text)
        }
        ByteArray::class -> spec.onLogEntry {
            method.call(containerInstance, it.bytes)
        }
        ContainerLogEntryContext::class -> spec.onLogEntry {
            method.call(containerInstance, it)
        }
        else -> {
            throw IllegalArgumentException(
                    "Method '$method' on class '${containerType.simpleName}' must expect a single param of valid type to receive log entries"
            )
        }
    }
    return spec
}
