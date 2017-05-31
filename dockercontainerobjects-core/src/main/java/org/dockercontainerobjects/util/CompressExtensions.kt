package org.dockercontainerobjects.util

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.io.IOUtils.toByteArray
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.URL

object CompressExtensions {

    @Throws(IOException::class)
    @JvmStatic
    fun TarArchiveOutputStream.withEntry(filename: String, content: ByteArray): TarArchiveOutputStream {
        val entry = TarArchiveEntry(filename)
        entry.size = content.size.toLong()
        putArchiveEntry(entry)
        write(content)
        closeArchiveEntry()
        return this
    }

    @Throws(IOException::class)
    @JvmStatic
    inline fun TarArchiveOutputStream.withEntry(filename: String, content: URI) = withEntry(filename, toByteArray(content))

    @Throws(IOException::class)
    @JvmStatic
    inline fun TarArchiveOutputStream.withEntry(filename: String, content: URL) = withEntry(filename, toByteArray(content))

    @Throws(IOException::class)
    @JvmStatic
    inline fun TarArchiveOutputStream.withEntry(filename: String, content: InputStream) = withEntry(filename, toByteArray(content))

    @Throws(IOException::class)
    @JvmStatic
    fun buildTAR(out: OutputStream, contentProducer: (TarArchiveOutputStream) -> Unit): OutputStream {
        val archive = TarArchiveOutputStream(out)
        archive.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
        try {
            contentProducer(archive)
            if (archive.bytesWritten == 0L)
                throw IllegalArgumentException("No entry written the the supplied content producer")
        } catch (e: RuntimeException) {
            val cause = e.cause
            if (cause !== null && cause is IOException)
                throw cause
            else
                throw e
        } finally {
            archive.close()
        }
        return out
    }

    @Throws(IOException::class)
    @JvmStatic
    fun buildTAR(contentProducer: (TarArchiveOutputStream) -> Unit, gzipped: Boolean = false): ByteArray {
        val buffer = ByteArrayOutputStream()
        val out =
            if (gzipped)
                GzipCompressorOutputStream(buffer)
            else
                buffer
        buildTAR(out, contentProducer).close()
        return buffer.toByteArray()
    }

    @Throws(IOException::class)
    @JvmStatic
    fun buildTARGZ(contentProducer: (TarArchiveOutputStream) -> Unit) = buildTAR(contentProducer, true)
}
