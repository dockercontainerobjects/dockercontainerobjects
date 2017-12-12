@file:JvmName("InetAddresses")
@file:Suppress("DEPRECATION", "NOTHING_TO_INLINE")

package org.dockercontainerobjects.docker.impl.dockerjava

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.security.AccessController
import java.security.PrivilegedAction
import com.github.dockerjava.api.model.ContainerNetwork as DJContainerNetwork
import com.github.dockerjava.api.model.NetworkSettings as DJNetworkSettings

inline fun String.toAddress(): InetAddress = InetAddress.getByName(this)

val preferIPv6: Boolean by lazy {
    val action: PrivilegedAction<Boolean> = PrivilegedAction {
        java.lang.Boolean.getBoolean("java.net.preferIPv6Addresses")
    }
    try {
        AccessController.doPrivileged(action)
    } catch (e: SecurityException) {
        false
    }
}

fun preferredAddress(ip4: String?, ip6: String?): InetAddress? =
        when {
            ip4 != null && ip6 != null -> if (preferIPv6) ip6.toAddress() else ip4.toAddress()
            ip4 != null && ip6 == null -> ip4.toAddress()
            ip4 == null && ip6 != null -> ip6.toAddress()
            else -> null
        }

inline val DJNetworkSettings.ip4Address get() = ipAddress?.toAddress() as Inet4Address
inline val DJNetworkSettings.ip6Address get() = globalIPv6Address?.toAddress() as Inet6Address
inline val DJNetworkSettings.preferredAddress get() = preferredAddress(ipAddress, globalIPv6Address)

inline val DJContainerNetwork.ip4Address get() = ipAddress?.toAddress() as Inet4Address
inline val DJContainerNetwork.ip6Address get() = globalIPv6Address?.toAddress() as Inet6Address
inline val DJContainerNetwork.preferredAddress get() = preferredAddress(ipAddress, globalIPv6Address)
