@file:JvmName("IO")
@file:Suppress("NOTHING_TO_INLINE")

package org.dockercontainerobjects.util

import java.io.InputStream
import org.apache.commons.io.IOUtils.toByteArray
import java.net.URI
import java.net.URL

inline fun InputStream.content(): ByteArray = toByteArray(this)
inline fun URL.content(): ByteArray = toByteArray(this)
inline fun URI.content(): ByteArray = toByteArray(this)
