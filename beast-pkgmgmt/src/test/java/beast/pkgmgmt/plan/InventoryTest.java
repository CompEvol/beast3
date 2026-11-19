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
    void pomDependenciesWithProperties() throws Exception {
        Path pom = dir.resolve("ccd-1.2.2.pom");
        Files.writeString(pom, """
                <project><version>1.2.2</version><properties><beast.version>2.8.0-rc1</beast.version></properties>
                <dependencies>
                  <dependency><groupId>io.github.compevol</groupId><artifactId>beast-base</artifactId><version>${beast.version}</version></dependency>
                  <dependency><groupId>io.github.compevol</groupId><artifactId>beast-base</artifactId><version>9.9</version><scope>test</scope></dependency>
                  <dependency><groupId>org.junit</groupId><artifactId>junit</artifactId><version>5</version></dependency>
                </dependencies></project>""");
        List<String[]> deps = Inventory.pomDependencies(pom, dir);
        assertEquals(2, deps.size());
        assertEquals(List.of("io.github.compevol", "beast-base", "2.8.0-rc1"), List.of(deps.get(0)));
    }

    /** a range set in a parent POM's properties */
    @Test
    void pomPropertiesFromTheParent() throws Exception {
        Path parentDir = Files.createDirectories(dir.resolve("org/x/parent/1"));
        Files.writeString(parentDir.resolve("parent-1.pom"),
                "<project><properties><beast.range>[2.8.0-rc3,2.9.0-alpha)</beast.range></properties></project>");
        Path pom = dir.resolve("child-1.pom");
        Files.writeString(pom, """
                <project><parent><groupId>org.x</groupId><artifactId>parent</artifactId><version>1</version></parent>
                <dependencies><dependency><groupId>io.github.compevol</groupId><artifactId>beast-base</artifactId>
                <version>${beast.range}</version></dependency></dependencies></project>""");
        assertEquals("[2.8.0-rc3,2.9.0-alpha)", Inventory.pomDependencies(pom, dir).get(0)[2]);
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

    private static Path jar(Path file, String versionXml) throws Exception {
        Files.createDirectories(file.getParent());
        try (java.util.jar.JarOutputStream out = new java.util.jar.JarOutputStream(Files.newOutputStream(file))) {
            out.putNextEntry(new java.util.jar.JarEntry("version.xml"));
            out.write(versionXml.getBytes());
            out.closeEntry();
        }
        return file;
    }

    /**
     * For a Maven package the POM states its dependencies: the core range, and other BEAST
     * packages (a dependency whose jar has a version.xml); version.xml's own depends only warn.
     */
    @Test
    void mavenPackageDependenciesComeFromItsPom() throws Exception {
        Path repo = dir.resolve("maven-repo");
        Path ccdDir = Files.createDirectories(repo.resolve("io/github/compevol/ccd/1.3.0"));
        Files.writeString(ccdDir.resolve("ccd-1.3.0.pom"), """
                <project><dependencies>
                  <dependency><groupId>io.github.compevol</groupId><artifactId>beast-base</artifactId><version>[2.8.0-rc3,2.9.0-alpha)</version></dependency>
                  <dependency><groupId>io.github.compevol</groupId><artifactId>sampled-ancestors</artifactId><version>2.3.0</version></dependency>
                  <dependency><groupId>org.apache.commons</groupId><artifactId>commons-text</artifactId><version>1.12.0</version></dependency>
                </dependencies></project>""");
        Path ccd = jar(ccdDir.resolve("ccd-1.3.0.jar"),
                "<package name='CCD' version='1.3.0'><depends on='BEAST.base' atleast='2.8.0'/></package>");
        Path sa = jar(repo.resolve("io/github/compevol/sampled-ancestors/2.3.0/sampled-ancestors-2.3.0.jar"),
                "<package name='SA' version='2.3.0'/>");
        Files.writeString(dir.resolve("maven-packages.xml"),
                "<packages><package groupId='io.github.compevol' artifactId='ccd' version='1.3.0'/></packages>");
        Inventory inventory = new Inventory().addMavenPackages(dir, coordinate -> List.of(ccd, sa));
        assertTrue(inventory.problems().isEmpty(), inventory.problems().toString());
        InstalledPackage p = inventory.packages().get(0);
        assertEquals(List.of(Dependency.fromMaven("BEAST.base", "[2.8.0-rc3,2.9.0-alpha)"), Dependency.fromMaven("SA", "2.3.0")),
                p.depends());
        assertEquals(List.of("CCD 1.3.0: its version.xml says it needs BEAST.base 2.8.0 or later, its POM says "
                + "BEAST.base [2.8.0-rc3,2.9.0-alpha); for a Maven package the POM counts"), inventory.warnings());
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
