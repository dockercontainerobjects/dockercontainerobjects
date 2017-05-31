package org.dockercontainerobjects.util;

import static org.dockercontainerobjects.util.CompressExtensions.buildTAR;
import static org.dockercontainerobjects.util.CompressExtensions.withEntry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import kotlin.Unit;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("Compression util tests")
@Tag("util")
public class CompressExtensionsTest {

    static final String TEST_FILENAME = "test.txt";
    static final String TEST_CONTENT = "sample";

    @Test
    @DisplayName("withEntry should fail if any parameter is null")
    void allArgsRequiredForEntryCreation() {
        assertThrows(IllegalArgumentException.class, () -> withEntry(null, TEST_FILENAME, new byte[]{}));
        assertThrows(IllegalArgumentException.class, () -> withEntry(new TarArchiveOutputStream(new ByteArrayOutputStream()), null, new byte[]{}));
        assertThrows(IllegalArgumentException.class, () -> withEntry(new TarArchiveOutputStream(new ByteArrayOutputStream()), TEST_FILENAME, (byte[])null));
    }

    @Test
    @DisplayName("TAR with null content provider should fail")
    void tarWithNullProviderShouldFail() {
        assertThrows(IllegalArgumentException.class, () -> buildTAR(null, false));
    }

    @Test
    @DisplayName("TAR with no content should fail")
    void tarWithNoContentShouldFail() {
        assertThrows(IllegalArgumentException.class, () -> buildTAR(
                tar -> Unit.INSTANCE, false));
    }

    @Test
    @DisplayName("TAR with one entry should work")
    void tarWithContentShouldWork() {
        try {
            byte[] data =
                    buildTAR(tar -> {
                        try {
                            TarArchiveEntry entry = new TarArchiveEntry(TEST_FILENAME);
                            byte[] content = TEST_CONTENT.getBytes(StandardCharsets.UTF_8);
                            entry.setSize(content.length);
                            tar.putArchiveEntry(entry);
                            tar.write(content);
                            tar.closeArchiveEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return Unit.INSTANCE;
                    }, false);
            assertNotNull(data);
            ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(new ByteArrayInputStream(data));
            assertNotNull(in);
            assertTrue(in instanceof TarArchiveInputStream);
            ArchiveEntry entry = in.getNextEntry();
            assertNotNull(in);
            assertEquals(TEST_FILENAME, entry.getName());
            byte[] expected = TEST_CONTENT.getBytes(StandardCharsets.UTF_8);
            int size = (int)entry.getSize();
            assertEquals(expected.length, size);
            byte[] content = new byte[size];
            int count = in.read(content);
            assertEquals(expected.length, count);
            assertArrayEquals(expected, content);
        } catch (ArchiveException|IOException e) {
            fail(e);
        }
    }
}
