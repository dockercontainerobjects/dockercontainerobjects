@file:JvmName("Archives")
@file:Suppress("NOTHING_TO_INLINE")

package org.dockercontainerobjects.util

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.URL

@Throws(IOException::class)
fun TarArchiveOutputStream.withEntry(entryname: String, content: ByteArray): TarArchiveOutputStream {
    val entry = TarArchiveEntry(entryname)
    entry.size = content.size.toLong()
    putArchiveEntry(entry)
    write(content)
    closeArchiveEntry()
    return this
}

@Throws(IOException::class)
inline fun TarArchiveOutputStream.withEntry(entryname: String, content: URI) =
        withEntry(entryname, content.content())

@Throws(IOException::class)
inline fun TarArchiveOutputStream.withEntry(entryname: String, content: URL) =
        withEntry(entryname, content.content())

@Throws(IOException::class)
inline fun TarArchiveOutputStream.withEntry(entryname: String, content: InputStream) =
        withEntry(entryname, content.content())

@Throws(IOException::class)
inline fun TarArchiveOutputStream.withEntry(entryname: String, content: Any) =
        when (content) {
            is ByteArray -> withEntry(entryname, content)
            is InputStream -> withEntry(entryname, content)
            is URL -> withEntry(entryname, content)
            is URI -> withEntry(entryname, content)
            else -> throw IllegalArgumentException("Unsupported type ${content.javaClass.name}")
        }

@Throws(IOException::class)
fun OutputStream.tar(producer: (TarArchiveOutputStream) -> Unit): OutputStream {
    val archive = TarArchiveOutputStream(this)
    archive.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
    try {
        producer(archive)
        if (archive.bytesWritten == 0L) {
            throw IllegalArgumentException("No entry written the the supplied content producer")
        }
    } catch (e: RuntimeException) {
        val cause = e.cause
        throw if (cause !== null && cause is IOException) cause else e
    } finally {
        archive.close()
    }
    return this
}

@Throws(IOException::class)
fun tar(gzipped: Boolean = false, producer: (TarArchiveOutputStream) -> Unit): ByteArray {
    val buffer = ByteArrayOutputStream()
    val out = if (gzipped) GzipCompressorOutputStream(buffer) else buffer
    out.tar(producer).close()
    return buffer.toByteArray()
}

@Throws(IOException::class)
inline fun tar(gzipped: Boolean = false, entries: Map<String, Any>) =
        tar(gzipped) { tar ->
            entries.forEach { name, content ->
                tar.withEntry(name, content)
            }
        }

@Throws(IOException::class)
inline fun targz(noinline producer: (TarArchiveOutputStream) -> Unit) = tar(true, producer)

@Throws(IOException::class)
inline fun targz(entries: Map<String, Any>) =
        targz { tar ->
            entries.forEach { name, content ->
                tar.withEntry(name, content)
            }
        }
