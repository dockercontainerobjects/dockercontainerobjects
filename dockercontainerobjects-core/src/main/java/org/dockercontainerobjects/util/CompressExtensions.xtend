package org.dockercontainerobjects.util

import static extension org.apache.commons.io.IOUtils.toByteArray

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.net.URL
import java.util.function.Consumer
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream

class CompressExtensions {

    public static def withEntry(TarArchiveOutputStream out, String filename, byte[] content) throws IOException {
        val entry = new TarArchiveEntry(filename)
        entry.size = content.length
        out.putArchiveEntry(entry)
        out.write(content)
        out.closeArchiveEntry
        out
    }

    public static def withEntry(TarArchiveOutputStream out, String filename, URI content) throws IOException {
        out.withEntry(filename, content.toByteArray)
    }

    public static def withEntry(TarArchiveOutputStream out, String filename, URL content) throws IOException {
        out.withEntry(filename, content.toByteArray)
    }

    public static def withEntry(TarArchiveOutputStream out, String filename, InputStream content) throws IOException {
        out.withEntry(filename, content.toByteArray)
    }

    public static def buildTAR(OutputStream out, Consumer<TarArchiveOutputStream> contentProducer) throws IOException {
        val archive = new TarArchiveOutputStream(out)
        archive.longFileMode = TarArchiveOutputStream.LONGFILE_GNU
        try {
            contentProducer.accept(archive)
            if (archive.bytesWritten == 0)
                throw new IllegalArgumentException("No entry written the the supplied content producer")
        } catch (RuntimeException e) {
            val cause = e.cause
            if (cause !== null && cause instanceof IOException)
                throw cause
            else
                throw e
        } finally {
            archive.close
        }
        out
    }

    public static def buildTAR(Consumer<TarArchiveOutputStream> contentProducer, boolean gzipped) throws IOException {
        val buffer = new ByteArrayOutputStream
        val out =
            if (gzipped)
                new GzipCompressorOutputStream(buffer)
            else
                buffer
        buildTAR(out, contentProducer).close
        buffer.toByteArray
    }

    public static def buildTARGZ(Consumer<TarArchiveOutputStream> contentProducer) throws IOException {
        buildTAR(contentProducer, true)
    }
}
