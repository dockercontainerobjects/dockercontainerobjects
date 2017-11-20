package org.dockercontainerobjects.extensions

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.UnknownHostException
import javax.net.SocketFactory

class ProxiedInetSocketFactory(val proxy: Proxy): SocketFactory() {

    @Throws(IOException::class)
    override fun createSocket() = Socket(proxy)

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int): Socket {
        val socket = createSocket()
        socket.connect(InetSocketAddress(host, port))
        return socket
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress?, localPort: Int): Socket {
        val socket = createSocket()
        socket.bind(InetSocketAddress(localHost, localPort))
        socket.connect(InetSocketAddress(host, port))
        return socket
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress?, port: Int): Socket {
        val socket = createSocket()
        socket.connect(InetSocketAddress(address, port))
        return socket
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket {
        val socket = createSocket()
        socket.bind(InetSocketAddress(localAddress, localPort))
        socket.connect(InetSocketAddress(address, port))
        return socket
    }
}
