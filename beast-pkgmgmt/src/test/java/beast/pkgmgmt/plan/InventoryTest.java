package beast.pkgmgmt.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import beast.pkgmgmt.plan.InstalledPackage.Location;

/** Reading installs: version.xml, POMs, and the folder layout. */
class InventoryTest {

    @TempDir
    Path dir;

    /** ccd 1.2.2's POM: the core version is a property; test dependencies do not count */
    @Test
    void coreVersionsFromPomProperties() throws Exception {
        Path pom = dir.resolve("ccd-1.2.2.pom");
        Files.writeString(pom, """
                <project><properties><beast.version>2.8.0-rc1</beast.version></properties>
                <dependencies>
                  <dependency><groupId>io.github.compevol</groupId><artifactId>beast-base</artifactId><version>${beast.version}</version></dependency>
                  <dependency><groupId>io.github.compevol</groupId><artifactId>beast-fx</artifactId><version>${beast.version}</version></dependency>
                  <dependency><groupId>io.github.compevol</groupId><artifactId>beast-base</artifactId><version>9.9</version><scope>test</scope></dependency>
                  <dependency><groupId>org.junit</groupId><artifactId>junit</artifactId><version>5</version></dependency>
                </dependencies></project>""");
        Map<String, Version> core = Inventory.coreVersionsInPom(pom);
        assertEquals(Map.of("BEAST.base", Version.of("2.8.0-rc1"), "BEAST.app", Version.of("2.8.0-rc1")), core);
    }

    /** BEAST.app 2.8.0's version.xml declares a dependency on itself; that is ignored */
    @Test
    void packageFolderWithoutJars() throws Exception {
        Path app = Files.createDirectories(dir.resolve("BEAST.app"));
        Files.writeString(app.resolve("version.xml"), """
                <package name="BEAST.app" version="2.8.0">
                  <depends on="BEAST.app"/>
                  <depends on="BEAST.base" atleast="2.8.0" atmost="2.8.0"/>
                </package>""");
        Files.createDirectories(dir.resolve("not-a-package"));
        Inventory inventory = new Inventory().addFolder(dir, Location.USER);
        assertTrue(inventory.problems().isEmpty(), inventory.problems().toString());
        assertEquals(1, inventory.packages().size());
        InstalledPackage p = inventory.packages().get(0);
        assertEquals("BEAST.app", p.name());
        assertEquals(List.of(new Dependency("BEAST.base", Version.of("2.8.0"), Version.of("2.8.0"))), p.depends());
        assertEquals(List.of("BEAST.app: its version.xml declares a dependency on BEAST.app itself; it is ignored"),
                inventory.warnings());
    }

    /** a dependency named in a comment is not a dependency */
    @Test
    void commentsAreNotDependencies() throws Exception {
        Path app = Files.createDirectories(dir.resolve("BEAST.app"));
        Files.writeString(app.resolve("version.xml"), """
                <!-- ~60 published packages declare <depends on="BEAST.app">, so renaming would orphan them -->
                <package name="BEAST.app" version="2.8.0"/>""");
        Inventory inventory = new Inventory().addFolder(dir, Location.USER);
        assertEquals(List.of(), inventory.packages().get(0).depends());
        assertTrue(inventory.warnings().isEmpty(), inventory.warnings().toString());
    }

    /** a service provider in version.xml whose class is not in the package: warned, still recorded */
    @Test
    void serviceProviderNotInTheJars() throws Exception {
        Path sa = Files.createDirectories(dir.resolve("SA"));
        Files.writeString(sa.resolve("version.xml"), """
                <package name="SA" version="2.3.0">
                  <service type="beast.base.core.BEASTInterface"><provider classname="sa.Gone"/></service>
                </package>""");
        Inventory inventory = new Inventory().addFolder(dir, Location.USER);
        assertEquals(java.util.Map.of("beast.base.core.BEASTInterface", java.util.Set.of("sa.Gone")),
                inventory.packages().get(0).services());
        assertEquals(List.of("SA 2.3.0: its version.xml lists 1 service provider(s) that are not in its jars, e.g. sa.Gone"),
                inventory.warnings());
    }

    /** ccd 1.2.2: version.xml says BEAST.base 2.8.0 or later, the POM was built against 2.8.0-rc1 */
    @Test
    void versionXmlAndPomDisagreeOnTheCore() throws Exception {
        Path repo = Files.createDirectories(dir.resolve("maven-repo/io/github/compevol/ccd/1.2.2"));
        Files.writeString(repo.resolve("ccd-1.2.2.pom"), """
                <project><properties><beast.version>2.8.0-rc1</beast.version></properties><dependencies>
                  <dependency><artifactId>beast-base</artifactId><version>${beast.version}</version></dependency>
                </dependencies></project>""");
        Path jar = repo.resolve("ccd-1.2.2.jar");
        try (java.util.jar.JarOutputStream out = new java.util.jar.JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new java.util.jar.JarEntry("version.xml"));
            out.write("<package name='CCD' version='1.2.2'><depends on='BEAST.base' atleast='2.8.0'/></package>".getBytes());
            out.closeEntry();
        }
        Files.writeString(dir.resolve("maven-packages.xml"),
                "<packages><package groupId='io.github.compevol' artifactId='ccd' version='1.2.2'/></packages>");
        Inventory inventory = new Inventory().addMavenPackages(dir, coordinate -> List.of(jar));
        assertTrue(inventory.problems().isEmpty(), inventory.problems().toString());
        InstalledPackage ccd = inventory.packages().get(0);
        assertEquals("CCD", ccd.name());
        assertEquals(List.of(new Dependency("BEAST.base", Version.of("2.8.0"), null)), ccd.depends(), "version.xml counts");
        assertEquals(List.of("CCD 1.2.2: version.xml needs BEAST.base 2.8.0 or later, but its POM was built against "
                + "2.8.0-rc1; version.xml counts"), inventory.warnings());
    }

    @Test
    void unreadableVersionXmlIsAProblemNotAFailure() throws Exception {
        Path bad = Files.createDirectories(dir.resolve("Bad"));
        Files.writeString(bad.resolve("version.xml"), "<package name='Bad'");
        Inventory inventory = new Inventory().addFolder(dir, Location.USER);
        assertTrue(inventory.packages().isEmpty());
        assertEquals(1, inventory.problems().size());
    }

    @Test
    void javafxVersionFromTheJreReleaseFile() throws Exception {
        Files.writeString(dir.resolve("release"), "JAVA_VERSION=\"25.0.3\"\nMODULES=\"java.base javafx.base javafx.controls\"\n");
        assertEquals(Version.of("25.0.3"), Inventory.javafxVersion(dir));
        Files.writeString(dir.resolve("release"), "JAVA_VERSION=\"25.0.3\"\nMODULES=\"java.base java.xml\"\n");
        assertEquals(null, Inventory.javafxVersion(dir));
    }
}
