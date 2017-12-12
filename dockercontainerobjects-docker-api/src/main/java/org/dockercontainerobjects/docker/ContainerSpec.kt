package org.dockercontainerobjects.docker

class ContainerSpec(val image: ImageLocator) {

    private val _labels = mutableMapOf<String, String>()
    private var _environment = mutableMapOf<String, String>()
    private var _entrypoint = mutableListOf<String>()
    private var _cmd = mutableListOf<String>()

    var name: String? = null
    val labels: Map<String, String> get() = _labels
    val environment get() = _environment
    val entrypoint get() = _entrypoint
    val cmd get() = _cmd

    fun withName(name: String) = this.also { it.name = name }

    fun withLabel(name: String, value: String) = this.also { _labels[name] = value }
    fun withLabel(entry: Pair<String, String>) = this.also { _labels += entry }
    fun withLabels(labels: Map<String, String>) = this.also { _labels += labels }

    fun withEnvironmentVariable(name: String, value: String) =
            this.also { _environment[name] = value }
    fun withEnvironmentVariable(entry: Pair<String, String>) =
            this.also { _environment[entry.first] = entry.second }
    fun withEnvironmentVariables(env: Map<String, String>) =
            this.also { _environment.putAll(env) }

    fun withEntryPoint(vararg entrypoint: String) =
            this.also { _entrypoint.addAll(entrypoint) }
    fun withEntryPoint(entrypoint: List<String>) =
            this.also { _entrypoint.addAll(entrypoint) }

    fun withCommand(vararg cmd: String) = this.also { _cmd.addAll(cmd) }
    fun withCommand(cmd: List<String>) = this.also { _cmd.addAll(cmd) }
}
