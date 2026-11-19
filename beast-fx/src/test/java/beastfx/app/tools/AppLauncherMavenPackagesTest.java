package beastfx.app.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import beast.pkgmgmt.PackageManager;
import beast.pkgmgmt.PackageManager.MavenCoordinate;

/**
 * AppLauncher lists the apps of packages installed from Maven (#82): their version.xml is
 * inside the package's JAR in maven-repo, not in a package directory. The package directory
 * is set up here as a Maven install leaves it, so no Maven resolver is needed.
 */
class AppLauncherMavenPackagesTest {

	@TempDir
	Path packageDir;

	private String previousPackageDir;

	@BeforeEach
	void usePackageDir() {
		previousPackageDir = System.getProperty("beast.user.package.dir");
		System.setProperty("beast.user.package.dir", packageDir.toString());
	}

	@AfterEach
	void restorePackageDir() {
		if (previousPackageDir == null) {
			System.clearProperty("beast.user.package.dir");
		} else {
			System.setProperty("beast.user.package.dir", previousPackageDir);
		}
	}

	@Test
	void appsOfMavenPackagesAreListed() throws Exception {
		MavenCoordinate rootXml = new MavenCoordinate("io.example", "apps-pkg", "1.0.0");
		MavenCoordinate metaInfXml = new MavenCoordinate("io.example.other", "meta-pkg", "2.1");
		MavenCoordinate notDownloaded = new MavenCoordinate("io.example", "missing-pkg", "1.0.0");
		PackageManager.saveMavenPackageConfig(List.of(rootXml, metaInfXml, notDownloaded));

		// version.xml at the JAR root, as in CCD
		writeJar(rootXml, "version.xml", """
				<package name='AppsPkg' version='1.0.0'>
				    <depends on='BEAST.base' atleast='2.8.0'/>
				    <packageapp description="First App" class="example.apps.FirstApp" args="-x 1"/>
				    <packageapp description="Second App" class="example.apps.SecondApp" args=""/>
				</package>""");
		// version.xml at META-INF/beast/version.xml
		writeJar(metaInfXml, "META-INF/beast/version.xml", """
				<package name='MetaPkg' version='2.1'>
				    <packageapp description="Meta App" class="example.meta.MetaApp" args=""/>
				</package>""");

		Map<String, PackageApp> apps = appsByClass();

		PackageApp first = apps.get("example.apps.FirstApp");
		assertTrue(first != null, "app of a Maven package with version.xml at the JAR root not listed: " + apps.keySet());
		assertEquals("AppsPkg", first.packageName);
		assertEquals("First App", first.description);
		assertEquals(List.of("-x", "1"), List.of(first.getArgs()));
		assertTrue(apps.containsKey("example.apps.SecondApp"), "second app not listed: " + apps.keySet());

		PackageApp meta = apps.get("example.meta.MetaApp");
		assertTrue(meta != null, "app of a Maven package with META-INF/beast/version.xml not listed: " + apps.keySet());
		assertEquals("MetaPkg", meta.packageName);
	}

	/** a package both in a package directory and installed from Maven lists its apps once */
	@Test
	void appInPackageDirectoryAndMavenIsListedOnce() throws Exception {
		String versionXml = """
				<package name='AppsPkg' version='1.0.0'>
				    <packageapp description="First App" class="example.apps.FirstApp" args=""/>
				</package>""";
		MavenCoordinate coord = new MavenCoordinate("io.example", "apps-pkg", "1.0.0");
		PackageManager.saveMavenPackageConfig(List.of(coord));
		writeJar(coord, "version.xml", versionXml);
		Path unpacked = Files.createDirectories(packageDir.resolve("AppsPkg"));
		Files.writeString(unpacked.resolve("version.xml"), versionXml);

		long n = new AppLauncher().getPackageApps().stream()
				.filter(a -> a.className.equals("example.apps.FirstApp")).count();
		assertEquals(1, n, "app listed " + n + " times");
	}

	@Test
	void mavenPackageJarIsFoundInMavenRepoLayout() throws Exception {
		MavenCoordinate coord = new MavenCoordinate("io.github.compevol", "ccd", "1.2.2");
		assertEquals(null, PackageManager.getMavenPackageJar(coord), "JAR found before it is downloaded");
		writeJar(coord, "version.xml", "<package name='CCD' version='1.2.2'/>");
		File jar = PackageManager.getMavenPackageJar(coord);
		assertEquals(packageDir.resolve("maven-repo/io/github/compevol/ccd/1.2.2/ccd-1.2.2.jar").toFile(), jar);
		assertEquals("CCD", PackageManager.readVersionXmlFromJar(jar).getDocumentElement().getAttribute("name"));
	}

	@Test
	void noMavenPackagesListsNoMavenApps() {
		assertFalse(appsByClass().containsKey("example.apps.FirstApp"));
	}

	private Map<String, PackageApp> appsByClass() {
		return new AppLauncher().getPackageApps().stream()
				.collect(Collectors.toMap(a -> a.className, a -> a, (a, b) -> a));
	}

	/** writes the package's main JAR where a Maven install puts it, holding one entry */
	private void writeJar(MavenCoordinate coord, String entryName, String content) throws Exception {
		Path dir = packageDir.resolve("maven-repo").resolve(coord.groupId.replace('.', '/'))
				.resolve(coord.artifactId).resolve(coord.version);
		Files.createDirectories(dir);
		File jar = dir.resolve(coord.artifactId + "-" + coord.version + ".jar").toFile();
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
			out.putNextEntry(new ZipEntry(entryName));
			out.write(content.getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
	}
}
