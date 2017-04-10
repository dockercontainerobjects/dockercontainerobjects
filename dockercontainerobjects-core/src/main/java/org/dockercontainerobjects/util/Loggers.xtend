package org.dockercontainerobjects.util

import java.util.function.Supplier
import org.slf4j.Logger

class Loggers {

    public static def trace(Logger l, Supplier<String> messageSupplier) {
        if (l.traceEnabled)
            l.trace(messageSupplier.get);
    }

    public static def trace(Logger l, Throwable e) {
        if (l.traceEnabled)
            l.trace(e.localizedMessage, e);
    }

    public static def debug(Logger l, Supplier<String> messageSupplier) {
        if (l.debugEnabled)
            l.debug(messageSupplier.get);
    }

    public static def debug(Logger l, Throwable e) {
        if (l.debugEnabled)
            l.debug(e.localizedMessage, e);
    }

    public static def info(Logger l, Supplier<String> messageSupplier) {
        if (l.infoEnabled)
            l.info(messageSupplier.get);
    }

    public static def info(Logger l, Throwable e) {
        if (l.infoEnabled)
            l.info(e.localizedMessage, e);
    }

    public static def warn(Logger l, Supplier<String> messageSupplier) {
        if (l.warnEnabled)
            l.warn(messageSupplier.get);
    }

    public static def warn(Logger l, Throwable e) {
        if (l.warnEnabled)
            l.warn(e.localizedMessage, e);
    }

    public static def error(Logger l, Supplier<String> messageSupplier) {
        if (l.errorEnabled)
            l.error(messageSupplier.get);
    }

    public static def error(Logger l, Throwable e) {
        if (l.errorEnabled)
            l.error(e.localizedMessage, e);
    }
}
