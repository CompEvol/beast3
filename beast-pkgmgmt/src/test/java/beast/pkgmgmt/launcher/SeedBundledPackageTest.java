package beast.pkgmgmt.launcher;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check of {@link BeastLauncher#seedBundledPackage(String)}: discovery
 * of a bundled package zip (via the launcher-jar parent walk), version parsing,
 * and extraction into the user package directory.
 *
 * <p>The test is a no-op unless a bundled zip is discoverable, i.e. a
 * {@code BEAST.base.package.v*.zip} has been staged under a {@code packages/}
 * directory in the launcher-jar parent chain (e.g. {@code beast-pkgmgmt/target/packages/}).
 * It therefore passes silently on CI and does real work when run locally after staging.
 */
public class SeedBundledPackageTest {

    @Test
    public void seedsBaseIntoUserDir() throws Exception {
        Path userDir = Files.createTempDirectory("beast-seed-test");
        String prev = System.getProperty("beast.user.package.dir");
        System.setProperty("beast.user.package.dir", userDir.toString());
        try {
            boolean handled = BeastLauncher.seedBundledPackage("BEAST.base");
            Assumptions.assumeTrue(handled,
                    "no bundled BEAST.base zip staged under a packages/ dir - skipping");

            Path versionXml = userDir.resolve("BEAST.base").resolve("version.xml");
            Path lib = userDir.resolve("BEAST.base").resolve("lib");
            assertTrue(Files.exists(versionXml), "version.xml should be extracted to the user dir");
            assertTrue(Files.isDirectory(lib), "lib/ should be extracted to the user dir");
            try (Stream<Path> jars = Files.list(lib)) {
                assertTrue(jars.anyMatch(p -> p.toString().endsWith(".jar")),
                        "lib/ should contain at least one jar");
            }
        } finally {
            if (prev == null) {
                System.clearProperty("beast.user.package.dir");
            } else {
                System.setProperty("beast.user.package.dir", prev);
            }
        }
    }
}
