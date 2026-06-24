package beast.pkgmgmt.launcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the BEAUti-template staleness bug (issue #112): a bundled
 * core package whose contents changed but whose version string did not (the
 * normal situation during a development cycle, where {@code version.xml} stays
 * e.g. {@code 2.8.0}) must still replace the stale copy already seeded into the
 * user package directory. A purely version-based check left the old {@code lib/}
 * in place, so the running app kept loading pre-fix classes.
 *
 * <p>Self-contained: it fabricates its own bundled package zips under a
 * {@code packages/} directory in the launcher-jar parent chain, using a private
 * package name so it never collides with real staged core packages, and removes
 * them afterwards.
 */
public class SeedBundledPackageReseedTest {

    private static final String PKG = "BEAST.reseedtest";

    private Path userDir;
    private String prevUserDir;
    private File packagesDir;
    private boolean createdPackagesDir;

    @BeforeEach
    public void setUp() throws Exception {
        userDir = Files.createTempDirectory("beast-reseed-user");
        prevUserDir = System.getProperty("beast.user.package.dir");
        System.setProperty("beast.user.package.dir", userDir.toString());

        // seedBundledPackage discovers zips by walking up from the launcher class
        // location looking for a packages/ directory; create one there.
        File codeSource = new File(BeastLauncher.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        packagesDir = new File(codeSource.getParentFile(), "packages");
        createdPackagesDir = !packagesDir.exists();
        packagesDir.mkdirs();
    }

    @AfterEach
    public void tearDown() throws Exception {
        File[] mine = packagesDir.listFiles((d, n) -> n.startsWith(PKG + ".package"));
        if (mine != null) {
            for (File f : mine) f.delete();
        }
        if (createdPackagesDir) {
            packagesDir.delete(); // only removes it if now empty
        }
        if (prevUserDir == null) {
            System.clearProperty("beast.user.package.dir");
        } else {
            System.setProperty("beast.user.package.dir", prevUserDir);
        }
        deleteRecursively(userDir.toFile());
    }

    @Test
    public void sameVersionDifferentContentTriggersReseed() throws Exception {
        writeBundle("2.8.0", "OLD");
        assertTrue(BeastLauncher.seedBundledPackage(PKG), "first seed should succeed");
        assertEquals("OLD", markerContent(), "first run extracts the bundled content");

        // rebuild: same version string, different content (the #112 scenario)
        writeBundle("2.8.0", "NEW");
        assertTrue(BeastLauncher.seedBundledPackage(PKG), "re-seed should succeed");
        assertEquals("NEW", markerContent(),
                "same version with new content must re-seed, not keep the stale copy");
    }

    @Test
    public void identicalBundleIsNotReExtracted() throws Exception {
        writeBundle("2.8.0", "A");
        BeastLauncher.seedBundledPackage(PKG);

        // mutate the extracted copy, then seed the *same* zip again
        Files.writeString(markerPath(), "LOCALLY-CHANGED");
        BeastLauncher.seedBundledPackage(PKG);
        assertEquals("LOCALLY-CHANGED", markerContent(),
                "an unchanged bundled zip must not be re-extracted");
    }

    @Test
    public void strictlyNewerUserCopyIsNotDowngraded() throws Exception {
        writeBundle("2.9.0", "USER_NEWER");
        BeastLauncher.seedBundledPackage(PKG);
        Files.writeString(markerPath(), "KEEP_ME");

        // bundled package is an older version -- must not clobber the user copy
        writeBundle("2.8.0", "BUNDLED_OLDER");
        BeastLauncher.seedBundledPackage(PKG);
        assertEquals("KEEP_ME", markerContent(),
                "a strictly newer user copy must never be downgraded");
    }

    // ---- helpers ----

    private Path markerPath() {
        return userDir.resolve(PKG).resolve("lib").resolve("marker.txt");
    }

    private String markerContent() throws Exception {
        return Files.readString(markerPath(), StandardCharsets.UTF_8);
    }

    /** Write <packagesDir>/BEAST.reseedtest.package.v<version>.zip holding a
     *  version.xml and a lib/marker.txt with the given content. The app ships
     *  exactly one version of each core package, so older test zips are removed. */
    private void writeBundle(String version, String content) throws Exception {
        File[] old = packagesDir.listFiles((d, n) -> n.startsWith(PKG + ".package"));
        if (old != null) {
            for (File f : old) f.delete();
        }
        File zip = new File(packagesDir, PKG + ".package.v" + version + ".zip");
        try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(zip))) {
            z.putNextEntry(new ZipEntry("version.xml"));
            z.write(("<package name=\"" + PKG + "\" version=\"" + version + "\"></package>")
                    .getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
            z.putNextEntry(new ZipEntry("lib/marker.txt"));
            z.write(content.getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
    }

    private static void deleteRecursively(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) deleteRecursively(c);
        }
        f.delete();
    }
}
