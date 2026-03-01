package test.beast.pkgmgmt;

import beast.pkgmgmt.PackageManager;
import beast.pkgmgmt.PackageManager.MavenCoordinate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Maven package config I/O ({@code maven-packages.xml}).
 * These tests override the package user dir to use a temp directory.
 */
class MavenConfigIOTest {

    @TempDir
    Path tempDir;

    /**
     * Point the package user dir at our temp dir for the duration of a test.
     */
    private void setPackageUserDir() {
        System.setProperty("beast.user.package.dir", tempDir.toString());
    }

    private void clearPackageUserDir() {
        System.clearProperty("beast.user.package.dir");
    }

    @Test
    void roundTripSaveLoad() {
        setPackageUserDir();
        try {
            List<MavenCoordinate> coords = new ArrayList<>();
            coords.add(new MavenCoordinate("io.github.alexeid", "beast-morph-models", "1.0.0"));
            coords.add(new MavenCoordinate("io.github.somedev", "beast-bsp", "2.1.0"));

            PackageManager.saveMavenPackageConfig(coords);

            // Verify the file was created
            File configFile = new File(tempDir.toFile(), "maven-packages.xml");
            assertTrue(configFile.exists(), "Config file should be created");

            // Load it back
            List<MavenCoordinate> loaded = PackageManager.loadMavenPackageConfig();
            assertEquals(2, loaded.size());

            assertEquals("io.github.alexeid", loaded.get(0).groupId);
            assertEquals("beast-morph-models", loaded.get(0).artifactId);
            assertEquals("1.0.0", loaded.get(0).version);

            assertEquals("io.github.somedev", loaded.get(1).groupId);
            assertEquals("beast-bsp", loaded.get(1).artifactId);
            assertEquals("2.1.0", loaded.get(1).version);
        } finally {
            clearPackageUserDir();
        }
    }

    @Test
    void loadFromNonExistentFileReturnsEmpty() {
        setPackageUserDir();
        try {
            List<MavenCoordinate> loaded = PackageManager.loadMavenPackageConfig();
            assertTrue(loaded.isEmpty(), "Should return empty list when file doesn't exist");
        } finally {
            clearPackageUserDir();
        }
    }

    @Test
    void loadEmptyPackagesElement() throws Exception {
        setPackageUserDir();
        try {
            File configFile = new File(tempDir.toFile(), "maven-packages.xml");
            try (PrintWriter pw = new PrintWriter(new FileWriter(configFile))) {
                pw.println("<packages></packages>");
            }

            List<MavenCoordinate> loaded = PackageManager.loadMavenPackageConfig();
            assertTrue(loaded.isEmpty(), "Should return empty list for empty <packages/>");
        } finally {
            clearPackageUserDir();
        }
    }

    @Test
    void loadSkipsMalformedEntries() throws Exception {
        setPackageUserDir();
        try {
            File configFile = new File(tempDir.toFile(), "maven-packages.xml");
            try (PrintWriter pw = new PrintWriter(new FileWriter(configFile))) {
                pw.println("<packages>");
                // Valid entry
                pw.println("  <package groupId=\"g\" artifactId=\"a\" version=\"1.0\" />");
                // Missing version
                pw.println("  <package groupId=\"g2\" artifactId=\"a2\" />");
                // Missing artifactId
                pw.println("  <package groupId=\"g3\" version=\"2.0\" />");
                pw.println("</packages>");
            }

            List<MavenCoordinate> loaded = PackageManager.loadMavenPackageConfig();
            assertEquals(1, loaded.size(), "Only the valid entry should be loaded");
            assertEquals("g", loaded.get(0).groupId);
            assertEquals("a", loaded.get(0).artifactId);
            assertEquals("1.0", loaded.get(0).version);
        } finally {
            clearPackageUserDir();
        }
    }

    @Test
    void saveOverwritesPreviousFile() {
        setPackageUserDir();
        try {
            List<MavenCoordinate> coords1 = new ArrayList<>();
            coords1.add(new MavenCoordinate("g1", "a1", "1.0"));
            PackageManager.saveMavenPackageConfig(coords1);

            List<MavenCoordinate> coords2 = new ArrayList<>();
            coords2.add(new MavenCoordinate("g2", "a2", "2.0"));
            PackageManager.saveMavenPackageConfig(coords2);

            List<MavenCoordinate> loaded = PackageManager.loadMavenPackageConfig();
            assertEquals(1, loaded.size(), "Second save should overwrite first");
            assertEquals("g2", loaded.get(0).groupId);
        } finally {
            clearPackageUserDir();
        }
    }

    @Test
    void mavenCoordinateToString() {
        MavenCoordinate coord = new MavenCoordinate("io.github.alexeid", "beast-morph-models", "1.0.0");
        assertEquals("io.github.alexeid:beast-morph-models:1.0.0", coord.toString());
    }
}
