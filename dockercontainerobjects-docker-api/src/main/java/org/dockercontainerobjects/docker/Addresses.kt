package org.dockercontainerobjects.docker

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

interface Addresses {

    val preferred: InetAddress
    val ip4: Inet4Address?
    val ip6: Inet6Address?
}
