package org.dockercontainerobjects.docker

import java.net.InetAddress
import kotlin.reflect.KClass

interface NetworkInfo {

    val id: String
    val name: String
    val addresses: Addresses
}
