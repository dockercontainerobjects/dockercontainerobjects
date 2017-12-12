package org.dockercontainerobjects.docker

import java.net.InetAddress
import kotlin.reflect.KClass

interface NetworkSettings {

    val addresses: Addresses
    val networks: Map<String, NetworkInfo>
}
