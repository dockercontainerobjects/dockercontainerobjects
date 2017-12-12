package org.dockercontainerobjects.docker.impl.dockerjava

import org.dockercontainerobjects.docker.Addresses
import org.dockercontainerobjects.docker.NetworkInfo
import org.dockercontainerobjects.docker.NetworkSettings
import java.net.Inet4Address
import java.net.Inet6Address
import com.github.dockerjava.api.model.ContainerNetwork as DJContainerNetwork
import com.github.dockerjava.api.model.NetworkSettings as DJNetworkSettings

class DockerJavaNetworkSettings(val model: DJNetworkSettings): NetworkSettings {

    override val addresses: Addresses
        get() = object : Addresses {
            override val preferred
                get() = model.preferredAddress ?: throw IllegalStateException()
            override val ip4: Inet4Address? get() = model.ip4Address
            override val ip6: Inet6Address? get() = model.ip6Address
        }

    override val networks by lazy {
        model.networks?.mapValues {
            (name, model) -> DockerJavaNetworkInfo(name, model)
        }.orEmpty()
    }
}

class DockerJavaNetworkInfo(override val name: String, val model: DJContainerNetwork): NetworkInfo {

    override val id get() = model.networkID ?: throw IllegalStateException()

    override val addresses: Addresses
        get() = object : Addresses {
            override val preferred
                get() = model.preferredAddress ?: throw IllegalStateException()
            override val ip4: Inet4Address? get() = model.ip4Address
            override val ip6: Inet6Address? get() = model.ip6Address
        }
}
