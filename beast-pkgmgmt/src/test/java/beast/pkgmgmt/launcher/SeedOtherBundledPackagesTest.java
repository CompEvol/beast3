package beast.pkgmgmt.launcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Seeding of the packages bundled with the application other than the core packages
 * (issue #248). Like {@link SeedBundledPackageReseedTest}, it fabricates its own package
 * zips in a {@code packages/} directory next to the launcher classes, with private
 * package names, and removes them afterwards.
 */
public class SeedOtherBundledPackagesTest {

    private static final String A = "SeedTestA";
    private static final String B = "SeedTestB";

    private Path userDir;
    private String prevUserDir;
    private File packagesDir;
    private boolean createdPackagesDir;
    private final List<File> zips = new ArrayList<>();

    @BeforeEach
    public void setUp() throws Exception {
        userDir = Files.createTempDirectory("beast-seed-other-user");
        prevUserDir = System.getProperty("beast.user.package.dir");
        System.setProperty("beast.user.package.dir", userDir.toString());
        File codeSource = new File(BeastLauncher.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        packagesDir = new File(codeSource.getParentFile(), "packages");
        createdPackagesDir = !packagesDir.exists();
        packagesDir.mkdirs();
    }

    @AfterEach
    public void tearDown() throws Exception {
        for (File f : zips) {
            f.delete();
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
    public void everyBundledPackageIsSeededUnderItsOwnName() throws Exception {
        // file names as Maven builds them; the package name comes from version.xml
        writeZip("seedtesta.v1.0.0.zip", A, "1.0.0", "a");
        writeZip("SeedTestB-1.2.zip", B, "1.2.0", "b");

        Set<String> handled = BeastLauncher.seedOtherBundledPackages();

        assertTrue(handled.contains(A) && handled.contains(B), "handled: " + handled);
        assertEquals("a", marker(A));
        assertEquals("b", marker(B));
        assertTrue(Files.exists(userDir.resolve(A).resolve("version.xml")));
        assertTrue(seededList().contains(A) && seededList().contains(B), "seeded list: " + seededList());
    }

    @Test
    public void removedPackageIsNotSeededAgain() throws Exception {
        writeZip("seedtesta.zip", A, "1.0.0", "a");
        writeZip("seedtestb.zip", B, "1.0.0", "b");
        BeastLauncher.seedOtherBundledPackages();

        // the user removes A, e.g. in the package manager
        deleteRecursively(userDir.resolve(A).toFile());
        Set<String> handled = BeastLauncher.seedOtherBundledPackages();

        assertFalse(Files.exists(userDir.resolve(A)), "a removed bundled package must not come back");
        assertFalse(handled.contains(A), "handled: " + handled);
        assertTrue(handled.contains(B), "the other package is still handled: " + handled);
        assertTrue(seededList().contains(A), "A stays on the list, so it stays removed");
    }

    @Test
    public void packageNewerInUserDirIsNotDowngraded() throws Exception {
        writeZip("seedtesta-2.zip", A, "2.0.0", "user-newer");
        BeastLauncher.seedOtherBundledPackages();
        Files.writeString(markerPath(A), "KEEP");

        // a later application bundles an older version than the one the user upgraded to
        zips.get(0).delete();
        zips.clear();
        writeZip("seedtesta-1.zip", A, "1.0.0", "bundled-older");
        BeastLauncher.seedOtherBundledPackages();

        assertEquals("KEEP", marker(A), "a newer user copy must never be downgraded");
    }

    @Test
    public void newerBundledVersionReplacesTheUserCopy() throws Exception {
        writeZip("seedtesta-1.zip", A, "1.0.0", "old");
        BeastLauncher.seedOtherBundledPackages();

        zips.get(0).delete();
        zips.clear();
        writeZip("seedtesta-2.zip", A, "2.0.0", "new");
        BeastLauncher.seedOtherBundledPackages();

        assertEquals("new", marker(A), "a newer bundled version replaces the seeded copy");
    }

    @Test
    public void corePackagesAndZipsWithoutVersionFileAreSkipped() throws Exception {
        // a zip claiming to be BEAST.app: the core packages are seeded by name elsewhere
        writeZip("coretest.zip", "BEAST.app", "99.0.0", "core");
        File noVersion = new File(packagesDir, "noversiontest.zip");
        try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(noVersion))) {
            z.putNextEntry(new ZipEntry("lib/readme.txt"));
            z.write("x".getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        zips.add(noVersion);

        Set<String> handled = BeastLauncher.seedOtherBundledPackages();

        assertFalse(handled.contains("BEAST.app"), "handled: " + handled);
        assertFalse(Files.exists(userDir.resolve("BEAST.app").resolve("lib").resolve("marker.txt")),
                "the core package must not be seeded from another zip");
    }

    @Test
    public void seedingANewPackageClearsTheCachedPackagePath() throws Exception {
        // a user who ran an earlier release has a cached class path without the package
        beast.pkgmgmt.Utils6.saveBeautiProperty("package.path", "cached-before-upgrade");
        writeZip("seedtesta.zip", A, "1.0.0", "a");
        BeastLauncher.seedOtherBundledPackages();
        assertEquals(null, beast.pkgmgmt.Utils6.getBeautiProperty("package.path"),
                "the cached package path must be cleared so that the new package is loaded");

        // nothing new on the next launch: the cache is left alone
        beast.pkgmgmt.Utils6.saveBeautiProperty("package.path", "cached-after");
        BeastLauncher.seedOtherBundledPackages();
        assertEquals("cached-after", beast.pkgmgmt.Utils6.getBeautiProperty("package.path"));
    }

    // ---- helpers ----

    private Path markerPath(String pkg) {
        return userDir.resolve(pkg).resolve("lib").resolve("marker.txt");
    }

    private String marker(String pkg) throws Exception {
        return Files.readString(markerPath(pkg), StandardCharsets.UTF_8);
    }

    private Set<String> seededList() throws Exception {
        return Set.copyOf(Files.readAllLines(userDir.resolve(BeastLauncher.SEEDED_BUNDLED_FILE)));
    }

    private void writeZip(String fileName, String pkg, String version, String content) throws Exception {
        File zip = new File(packagesDir, fileName);
        try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(zip))) {
            z.putNextEntry(new ZipEntry("version.xml"));
            z.write(("<package name='" + pkg + "' version='" + version + "'>\n"
                    + "    <depends on='BEAST.base' atleast='2.8.0'/>\n</package>\n").getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
            z.putNextEntry(new ZipEntry("lib/marker.txt"));
            z.write(content.getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        zips.add(zip);
    }

    private static void deleteRecursively(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteRecursively(c);
            }
        }
        f.delete();
    }
}
