package org.dockercontainerobjects.docker

import java.io.File

class ImageSpec private constructor() {

    private val _tags = mutableSetOf<ImageName>()
    private val _labels = mutableMapOf<String, String>()
    private var _descriptorFile: File? = null
    private var _imageContent: ByteArray? = null

    constructor(descriptorFile: File): this() {
        _descriptorFile = descriptorFile
    }

    constructor(imageContent: ByteArray): this() {
        _imageContent = imageContent
    }

    val dockerFile: File? get() = _descriptorFile
    val imageContent: ByteArray? get() = _imageContent
    val tags: Set<ImageName> get() = _tags
    val labels: Map<String, String> get() = _labels
    var pull: Boolean = false

    fun withTag(tag: ImageName) = this.also { _tags.add(tag) }
    fun withTags(vararg tags: ImageName) = this.also { _tags.addAll(tags) }

    fun withLabel(name: String, value: String) = this.also { _labels[name] = value }
    fun withLabel(entry: Pair<String, String>) = this.also { _labels += entry }
    fun withLabels(labels: Map<String, String>) = this.also { _labels += labels }

    fun withPull(pull: Boolean = true) = this.also { it.pull = pull }
}
