package test.beast.pkgmgmt;

import beast.pkgmgmt.PackageManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PackageManager#doUnzip}, including Zip Slip protection.
 */
class DoUnzipTest {

    @TempDir
    Path tempDir;

    /** Create a zip file with the given entry names and content. */
    private File createZip(String zipName, String... entryNames) throws IOException {
        File zipFile = tempDir.resolve(zipName).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (String name : entryNames) {
                zos.putNextEntry(new ZipEntry(name));
                zos.write(("content of " + name).getBytes());
                zos.closeEntry();
            }
        }
        return zipFile;
    }

    @Test
    void normalZipExtractsCorrectly() throws IOException {
        File zipFile = createZip("normal.zip", "lib/foo.jar", "version.xml");

        Path destDir = tempDir.resolve("output");
        Files.createDirectories(destDir);

        PackageManager.doUnzip(zipFile.getAbsolutePath(), destDir.toString());

        assertTrue(Files.exists(destDir.resolve("lib/foo.jar")));
        assertTrue(Files.exists(destDir.resolve("version.xml")));
        assertEquals("content of version.xml",
                Files.readString(destDir.resolve("version.xml")));
    }

    @Test
    void zipWithDirectoryEntriesCreatesDirectories() throws IOException {
        File zipFile = createZip("dirs.zip", "subdir/", "subdir/file.txt");

        Path destDir = tempDir.resolve("output2");
        Files.createDirectories(destDir);

        PackageManager.doUnzip(zipFile.getAbsolutePath(), destDir.toString());

        assertTrue(Files.isDirectory(destDir.resolve("subdir")));
        assertTrue(Files.exists(destDir.resolve("subdir/file.txt")));
    }

    @Test
    void zipSlipIsRejected() throws IOException {
        // Manually craft a zip with a path-traversal entry
        File zipFile = tempDir.resolve("evil.zip").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // This entry tries to escape the destination directory
            zos.putNextEntry(new ZipEntry("../../evil.txt"));
            zos.write("malicious content".getBytes());
            zos.closeEntry();
        }

        Path destDir = tempDir.resolve("output3");
        Files.createDirectories(destDir);

        IOException ex = assertThrows(IOException.class,
                () -> PackageManager.doUnzip(zipFile.getAbsolutePath(), destDir.toString()));
        assertTrue(ex.getMessage().contains("outside target directory"));
    }
}
