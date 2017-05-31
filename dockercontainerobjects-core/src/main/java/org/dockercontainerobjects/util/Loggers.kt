package org.dockercontainerobjects.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Loggers {

    inline fun <reified T: Any> loggerFor(): Logger = LoggerFactory.getLogger(T::class.java)

    inline fun Logger.trace(message: () -> String) {
        if (isTraceEnabled)
            trace(message())
    }

    inline fun Logger.trace(e: Throwable) {
        if (isTraceEnabled)
            trace(e.localizedMessage, e)
    }

    inline fun Logger.debug(message: () -> String) {
        if (isDebugEnabled)
            debug(message())
    }

    inline fun Logger.debug(e: Throwable) {
        if (isDebugEnabled)
            debug(e.localizedMessage, e)
    }

    inline fun Logger.info(message: () -> String) {
        if (isInfoEnabled)
            info(message())
    }

    inline fun Logger.info(e: Throwable) {
        if (isInfoEnabled)
            info(e.localizedMessage, e)
    }

    inline fun Logger.warn(message: () -> String) {
        if (isWarnEnabled)
            warn(message())
    }

    inline fun Logger.warn(e: Throwable) {
        if (isWarnEnabled)
            warn(e.localizedMessage, e)
    }

    inline fun Logger.error(message: () -> String) {
        if (isErrorEnabled)
            error(message())
    }

    inline fun Logger.error(e: Throwable) {
        if (isErrorEnabled)
            error(e.localizedMessage, e)
    }
}
