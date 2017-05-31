package org.dockercontainerobjects.util

import java.util.Optional

object Optionals {

    inline fun <T> T.confirmed() = Optional.of(this)

    inline fun <T> T?.unsure() = Optional.ofNullable(this)

    inline fun <T> Optional<T>.value(): T? = orElse(null)
}
