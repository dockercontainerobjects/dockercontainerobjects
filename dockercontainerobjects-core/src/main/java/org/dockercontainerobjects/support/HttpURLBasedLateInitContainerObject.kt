package org.dockercontainerobjects.support

import org.dockercontainerobjects.util.debug
import org.dockercontainerobjects.util.loggerFor
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

abstract class HttpURLBasedLateInitContainerObject : ScheduledCheckLateInitContainerObject() {

    companion object {
        private val l = loggerFor<HttpURLBasedLateInitContainerObject>()
    }

    abstract protected val serverReadyURL: URL
    protected val expectedReturnCode: Int = HttpURLConnection.HTTP_OK
    open protected val httpMethod get() = "HEAD"

    override fun isServerReady(): Boolean {
        try {
            l.debug { "sending a $httpMethod request to $serverReadyURL" }
            val conn = environment.openOnDockerNetwork(serverReadyURL) as HttpURLConnection
            conn.connectTimeout = maxTimeoutMillis
            conn.readTimeout = maxTimeoutMillis
            conn.requestMethod = httpMethod
            conn.connect()
            return conn.responseCode == expectedReturnCode
        } catch (ex: IOException) {
            return false
        }
    }
}
