package test.beast.pkgmgmt;

import beast.pkgmgmt.PackageManager;
import beast.pkgmgmt.PackageManager.MavenCoordinate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PackageManager#parseServicesFromJar(List, MavenCoordinate)}.
 */
class ParseServicesFromJarTest {

    @TempDir
    Path tempDir;

    @Test
    void parseVersionXmlFromJarRoot() throws Exception {
        // Create a JAR with version.xml at the root
        String versionXml = """
                <package name="test-package" version="1.0.0">
                    <service type="beast.base.evolution.datatype.DataType">
                        <provider classname="test.MyDataType"/>
                    </service>
                </package>
                """;

        Path jarPath = createJar("test-package-1.0.0.jar", "version.xml", versionXml);
        MavenCoordinate coord = new MavenCoordinate("com.test", "test-package", "1.0.0");

        Map<String, Set<String>> services =
                PackageManager.parseServicesFromJar(List.of(jarPath), coord);

        assertNotNull(services);
        assertTrue(services.containsKey("beast.base.evolution.datatype.DataType"));
        assertTrue(services.get("beast.base.evolution.datatype.DataType")
                .contains("test.MyDataType"));
    }

    @Test
    void parseVersionXmlFromMetaInf() throws Exception {
        // Create a JAR with version.xml at META-INF/beast/version.xml
        String versionXml = """
                <package name="test-package" version="2.0.0">
                    <service type="beast.base.core.BEASTInterface">
                        <provider classname="test.MyModel"/>
                        <provider classname="test.MyOtherModel"/>
                    </service>
                </package>
                """;

        Path jarPath = createJar("test-package-2.0.0.jar",
                "META-INF/beast/version.xml", versionXml);
        MavenCoordinate coord = new MavenCoordinate("com.test", "test-package", "2.0.0");

        Map<String, Set<String>> services =
                PackageManager.parseServicesFromJar(List.of(jarPath), coord);

        assertNotNull(services);
        Set<String> providers = services.get("beast.base.core.BEASTInterface");
        assertNotNull(providers);
        assertEquals(2, providers.size());
        assertTrue(providers.contains("test.MyModel"));
        assertTrue(providers.contains("test.MyOtherModel"));
    }

    @Test
    void parsePackageAppElements() throws Exception {
        String versionXml = """
                <package name="test-package" version="1.0.0">
                    <packageapp class="test.MyApp" description="Test application"/>
                </package>
                """;

        Path jarPath = createJar("test-package-1.0.0.jar", "version.xml", versionXml);
        MavenCoordinate coord = new MavenCoordinate("com.test", "test-package", "1.0.0");

        Map<String, Set<String>> services =
                PackageManager.parseServicesFromJar(List.of(jarPath), coord);

        assertNotNull(services);
        Set<String> apps = services.get("has.main.method");
        assertNotNull(apps);
        assertTrue(apps.contains("test.MyApp"));
    }

    @Test
    void noVersionXmlReturnsNull() throws Exception {
        // Create a JAR without version.xml
        Path jarPath = createJar("test-package-1.0.0.jar", "some/other/file.txt", "hello");
        MavenCoordinate coord = new MavenCoordinate("com.test", "test-package", "1.0.0");

        Map<String, Set<String>> services =
                PackageManager.parseServicesFromJar(List.of(jarPath), coord);

        assertNull(services, "Should return null when no version.xml found");
    }

    @Test
    void emptyJarListReturnsNull() {
        MavenCoordinate coord = new MavenCoordinate("com.test", "test-package", "1.0.0");

        Map<String, Set<String>> services =
                PackageManager.parseServicesFromJar(List.of(), coord);

        assertNull(services, "Should return null for empty JAR list");
    }

    @Test
    void matchesMainArtifactByName() throws Exception {
        // Create two JARs — only the one matching artifactId should be checked
        String versionXml = """
                <package name="main-pkg" version="1.0.0">
                    <service type="beast.base.core.BEASTInterface">
                        <provider classname="test.MainProvider"/>
                    </service>
                </package>
                """;

        Path mainJar = createJar("main-pkg-1.0.0.jar", "version.xml", versionXml);
        Path depJar = createJar("some-dependency-3.0.0.jar", "version.xml",
                "<package name=\"dep\"><service type=\"other\"><provider classname=\"other.Cls\"/></service></package>");

        MavenCoordinate coord = new MavenCoordinate("com.test", "main-pkg", "1.0.0");

        // main-pkg-1.0.0.jar should be selected as the main artifact
        Map<String, Set<String>> services =
                PackageManager.parseServicesFromJar(List.of(depJar, mainJar), coord);

        assertNotNull(services);
        assertTrue(services.containsKey("beast.base.core.BEASTInterface"));
        assertTrue(services.get("beast.base.core.BEASTInterface").contains("test.MainProvider"));
    }

    /**
     * Helper: create a JAR file in the temp directory with a single entry.
     */
    private Path createJar(String jarName, String entryName, String content) throws Exception {
        File jarFile = new File(tempDir.toFile(), jarName);
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            jos.putNextEntry(new JarEntry(entryName));
            jos.write(content.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        return jarFile.toPath();
    }
}
