package beast.pkgmgmt.plan;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import beast.pkgmgmt.plan.Artifact.Kind;
import beast.pkgmgmt.plan.InstalledPackage.Location;
import beast.pkgmgmt.plan.Runtime.BootModule;

/**
 * Reads what is installed into {@link InstalledPackage} records, whatever the format: ZIP
 * package folders, the bundled package zips of an installed application, and packages installed
 * from Maven. Also reads the {@link Runtime} of an installed application. Reading changes
 * nothing on disk, except that bundled zips are unpacked into a folder the caller gives.
 * <p>
 * Problems with one jar or package (an unreadable jar, a bad module name) are reported through
 * {@link #problems()} and skip that jar or package; they never stop the rest.
 */
public final class Inventory {

    private static final Pattern VERSION_IN_FILE_NAME = Pattern.compile("-(\\d[\\w.\\-]*?)\\.jar$");
    private static final Map<String, String> CORE_ARTIFACTS = Map.of("beast-base", Planner.BASE, "beast-fx", Planner.APP);

    private final List<InstalledPackage> packages = new ArrayList<>();
    private final List<String> problems = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public List<InstalledPackage> packages() {
        return packages;
    }

    public List<String> problems() {
        return problems;
    }

    /**
     * Where a package's version.xml, module-info and POM disagree, or say something only the
     * other should (decision 5): the package still loads; its author learns what to fix.
     */
    public List<String> warnings() {
        return warnings;
    }

    // ---- ZIP packages ----

    /** every package folder (a sub-folder with a version.xml) directly in {@code dir} */
    public Inventory addFolder(Path dir, Location location) {
        if (!Files.isDirectory(dir)) {
            return this;
        }
        try (Stream<Path> subs = Files.list(dir)) {
            for (Path sub : subs.sorted().toList()) {
                if (Files.isRegularFile(sub.resolve("version.xml"))) {
                    addPackageFolder(sub, location);
                }
            }
        } catch (IOException e) {
            problems.add(dir + ": " + e.getMessage());
        }
        return this;
    }

    /** one ZIP package folder: its version.xml and the jars in its lib/ */
    public Inventory addPackageFolder(Path dir, Location location) {
        try {
            Document doc = parse(Files.newInputStream(dir.resolve("version.xml")));
            Element root = doc.getDocumentElement();
            String name = root.getAttribute("name");
            Version version = Version.of(root.getAttribute("version"));
            List<Artifact> artifacts = new ArrayList<>();
            Path lib = dir.resolve("lib");
            if (Files.isDirectory(lib)) {
                try (Stream<Path> jars = Files.list(lib)) {
                    for (Path jar : jars.filter(p -> p.toString().endsWith(".jar")).sorted().toList()) {
                        Artifact a = readJar(jar, version, name);
                        if (a != null) {
                            artifacts.add(a);
                        }
                    }
                }
            }
            Map<String, Set<String>> services = services(doc);
            checkServices(name, version, services,
                    artifacts.stream().filter(a -> a.kind() == Kind.OWN).map(Artifact::jar).toList());
            packages.add(new InstalledPackage(name, version, location, dependencies(doc, name), artifacts, services));
        } catch (Exception e) {
            problems.add(dir + ": " + e.getMessage());
        }
        return this;
    }

    /**
     * The archive in a package folder, {@code archive/<name>/<version>/}: versions installed
     * with {@code packagemanager -add NAME -version V}, kept for {@code -strictversions}.
     */
    public Inventory addArchive(Path packageDir) {
        Path archive = packageDir.resolve("archive");
        if (!Files.isDirectory(archive)) {
            return this;
        }
        try (Stream<Path> names = Files.list(archive)) {
            for (Path name : names.filter(Files::isDirectory).sorted().toList()) {
                addFolder(name, Location.ARCHIVE);
            }
        } catch (IOException e) {
            problems.add(archive + ": " + e.getMessage());
        }
        return this;
    }


    // ---- Maven packages ----

    /**
     * The packages listed in {@code maven-packages.xml} in {@code packageDir}, each resolved to
     * its jars by {@code resolver} (coordinate "g:a:v" to the jar files, from the local
     * {@code maven-repo}). The main jar is the package's own module; the rest are libraries.
     * Name and dependencies come from the {@code version.xml} in the main jar, plus the
     * versions of BEAST's core in the POM, which are minimums.
     */
    public Inventory addMavenPackages(Path packageDir, Function<String, List<Path>> resolver) {
        Path config = packageDir.resolve("maven-packages.xml");
        if (!Files.isRegularFile(config)) {
            return this;
        }
        try {
            NodeList nodes = parse(Files.newInputStream(config)).getElementsByTagName("package");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                String g = e.getAttribute("groupId"), a = e.getAttribute("artifactId"), v = e.getAttribute("version");
                addMavenPackage(packageDir.resolve("maven-repo"), g, a, v, resolver);
            }
        } catch (Exception e) {
            problems.add(config + ": " + e.getMessage());
        }
        return this;
    }

    private void addMavenPackage(Path repo, String g, String a, String v, Function<String, List<Path>> resolver) {
        String coordinate = g + ":" + a + ":" + v;
        try {
            List<Path> jars = resolver.apply(coordinate);
            Path main = jars.stream().filter(p -> p.getFileName().toString().equals(a + "-" + v + ".jar")).findFirst()
                    .orElseThrow(() -> new IOException("main jar " + a + "-" + v + ".jar not among the resolved jars"));
            Document versionXml = readVersionXml(main);
            String name = versionXml != null ? versionXml.getDocumentElement().getAttribute("name") : a;
            Version version = Version.of(v);
            if (versionXml != null) {
                String declared = versionXml.getDocumentElement().getAttribute("version");
                if (!declared.isEmpty() && !Version.of(declared).equals(version)) {
                    warnings.add(name + " " + version + ": its version.xml says version " + declared
                            + ", but it is installed as " + coordinate + "; the Maven version counts");
                }
            }
            // a Maven package's dependencies are its POM's job (decision 5): the core and other
            // BEAST packages as minimums or ranges, checked and never downloaded (#298); the rest
            // are libraries, resolved by Maven
            List<Dependency> depends = new ArrayList<>();
            Path pom = repo.resolve(g.replace('.', '/')).resolve(a).resolve(v).resolve(a + "-" + v + ".pom");
            for (String[] dep : pomDependencies(pom, repo)) {
                String depGroup = dep[0], depArtifact = dep[1], spec = dep[2];
                String core = "io.github.compevol".equals(depGroup) ? CORE_ARTIFACTS.get(depArtifact) : null;
                String beastPackage = core;
                if (beastPackage == null && !"beast-pkgmgmt".equals(depArtifact)) {
                    beastPackage = jars.stream()
                            .filter(j -> j.toString().replace('\\', '/').contains("/" + depGroup.replace('.', '/') + "/" + depArtifact + "/"))
                            .map(j -> {
                                try {
                                    Document d = readVersionXml(j);
                                    return d == null ? null : d.getDocumentElement().getAttribute("name");
                                } catch (Exception e) {
                                    return null;
                                }
                            })
                            .filter(n -> n != null && !n.isEmpty()).findFirst().orElse(null);
                }
                if (beastPackage != null && spec != null) {
                    try {
                        depends.add(Dependency.fromMaven(beastPackage, spec));
                    } catch (IllegalArgumentException e) {
                        problems.add(coordinate + ": " + depGroup + ":" + depArtifact + " " + e.getMessage());
                    }
                }
            }
            if (versionXml != null) {
                for (Dependency stated : dependencies(versionXml, name)) {
                    Dependency inPom = depends.stream().filter(d -> d.name().equals(stated.name())).findFirst().orElse(null);
                    if (inPom == null || !inPom.equals(stated)) {
                        warnings.add(name + " " + version + ": its version.xml says it needs " + stated.describe(false)
                                + ", its POM " + (inPom == null ? "does not mention " + stated.name() : "says " + inPom.describe(false))
                                + "; for a Maven package the POM counts");
                    }
                }
            }
            List<Artifact> artifacts = new ArrayList<>();
            for (Path jar : jars) {
                Artifact art = readJar(jar, jar.equals(main) ? version : null, null);
                if (art != null) {
                    artifacts.add(jar.equals(main)
                            ? new Artifact(art.module(), version, jar, Kind.OWN, art.javaPackages(), art.requires())
                            : new Artifact(art.module(), art.version(), jar, Kind.LIBRARY, art.javaPackages(), art.requires()));
                }
            }
            Map<String, Set<String>> services = versionXml != null ? services(versionXml) : Map.of();
            checkServices(name, version, services, List.of(main));
            packages.add(new InstalledPackage(name, version, Location.MAVEN, depends, artifacts, services));
        } catch (Exception e) {
            problems.add(coordinate + ": " + e.getMessage());
        }
    }

    /**
     * The dependencies a published POM declares, other than test-scoped ones: groupId,
     * artifactId and version (a plain version or a range), with {@code ${...}} filled in from
     * its properties and those of its parent POMs in the local repository.
     */
    static List<String[]> pomDependencies(Path pom, Path repo) throws Exception {
        List<String[]> result = new ArrayList<>();
        if (!Files.isRegularFile(pom)) {
            return result;
        }
        Map<String, String> properties = new HashMap<>();
        Document doc = parse(Files.newInputStream(pom));
        for (Document d = doc; d != null; d = parentPom(d, repo)) {
            NodeList props = d.getElementsByTagName("properties");
            for (int i = 0; i < props.getLength(); i++) {
                for (Node n = props.item(i).getFirstChild(); n != null; n = n.getNextSibling()) {
                    if (n instanceof Element p) {
                        properties.putIfAbsent(p.getTagName(), p.getTextContent().trim());
                    }
                }
            }
        }
        Element project = doc.getDocumentElement();
        properties.putIfAbsent("project.version", String.valueOf(text(project, "version")));
        NodeList deps = doc.getElementsByTagName("dependency");
        for (int i = 0; i < deps.getLength(); i++) {
            Element d = (Element) deps.item(i);
            if (d.getParentNode() == null || !"dependencies".equals(d.getParentNode().getNodeName())
                    || (d.getParentNode().getParentNode() instanceof Element pe && pe.getTagName().equals("dependencyManagement"))) {
                continue;
            }
            String scope = text(d, "scope"), version = text(d, "version");
            if ("test".equals(scope)) {
                continue;
            }
            if (version != null) {
                Matcher m = Pattern.compile("\\$\\{([^}]+)}").matcher(version);
                version = m.replaceAll(r -> Matcher.quoteReplacement(properties.getOrDefault(r.group(1), r.group())));
                if (version.contains("${")) {
                    version = null;
                }
            }
            result.add(new String[] {text(d, "groupId"), text(d, "artifactId"), version});
        }
        return result;
    }

    /** the parent POM of a POM, from the local repository, or null */
    private static Document parentPom(Document doc, Path repo) throws Exception {
        for (Node n = doc.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element parent && parent.getTagName().equals("parent")) {
                String g = text(parent, "groupId"), a = text(parent, "artifactId"), v = text(parent, "version");
                if (g == null || a == null || v == null) {
                    return null;
                }
                Path p = repo.resolve(g.replace('.', '/')).resolve(a).resolve(v).resolve(a + "-" + v + ".pom");
                return Files.isRegularFile(p) ? parse(Files.newInputStream(p)) : null;
            }
        }
        return null;
    }

    // ---- the runtime of an installed application ----



    /** the runtime of this JVM, from its boot layer: what a {@link Loader} here builds on */
    public static Runtime readThisRuntime() {
        List<BootModule> boot = new ArrayList<>();
        Version launcher = Version.of("0"), javafx = null;
        for (Module m : ModuleLayer.boot().modules()) {
            ModuleDescriptor d = m.getDescriptor();
            boot.add(new BootModule(d.name(), d.packages()));
            Version v = Version.of(d.rawVersion().orElse("0"));
            if (d.name().equals("beast.pkgmgmt")) {
                launcher = v;
            } else if (d.name().equals("javafx.base")) {
                javafx = v;
            }
        }
        return new Runtime(launcher, javafx, boot);
    }

    /**
     * In developer mode the core is not installed as packages: BEAST.base and BEAST.app are
     * whatever is on the boot layer. Records them as {@link Location#BOOT} copies, as
     * {@code PackageManager.addInstalledPackages()} does today with its "synthetic" versions.
     */
    public Inventory addCoreOnBootLayer(Runtime runtime, Version version) {
        for (Map.Entry<String, String> core : Map.of("beast.base", Planner.BASE, "beast.fx", Planner.APP).entrySet()) {
            runtime.bootModules().stream().filter(b -> b.name().equals(core.getKey())).findFirst().ifPresent(b ->
                    packages.add(new InstalledPackage(core.getValue(), version, Location.BOOT, List.of(),
                            List.of(new Artifact(b.name(), version, null, Kind.OWN, b.javaPackages(), Set.of())))));
        }
        return this;
    }


    // ---- jars ----

    /**
     * One jar as an artifact. In a ZIP package ({@code packageName} given) a jar is the package's
     * own module when it is BEAST code: a named module called {@code beast.*}, or one that
     * requires a {@code beast.*} module; any other jar in {@code lib/} is a library.
     */
    Artifact readJar(Path jar, Version packageVersion, String packageName) {
        try {
            ModuleReference ref = ModuleFinder.of(jar).findAll().iterator().next();
            ModuleDescriptor d = ref.descriptor();
            Set<String> requires = d.requires().stream()
                    .filter(r -> !r.modifiers().contains(Requires.Modifier.STATIC))
                    .map(Requires::name)
                    .filter(n -> !n.startsWith("java.") && !n.startsWith("jdk."))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            boolean beastCode = !d.isAutomatic()
                    && (d.name().startsWith("beast.") || requires.stream().anyMatch(n -> n.startsWith("beast.")));
            boolean own = packageName != null && beastCode;
            Version version = own && packageVersion != null ? packageVersion : versionOf(d, jar);
            return new Artifact(d.name(), version, jar, own ? Kind.OWN : Kind.LIBRARY, d.packages(), requires);
        } catch (IOException | RuntimeException e) {
            problems.add(jar + ": not readable as a module: " + e.getMessage());
            return null;
        }
    }

    private static Version versionOf(ModuleDescriptor d, Path jar) throws IOException {
        if (d.rawVersion().isPresent()) {
            return Version.of(d.rawVersion().get());
        }
        Matcher m = VERSION_IN_FILE_NAME.matcher(jar.getFileName().toString());
        return Version.of(m.find() ? m.group(1) : "0");
    }

    private static Document readVersionXml(Path jar) throws Exception {
        try (JarFile jf = new JarFile(jar.toFile())) {
            JarEntry e = jf.getJarEntry("version.xml");
            if (e == null) {
                e = jf.getJarEntry("META-INF/beast/version.xml");
            }
            return e == null ? null : parse(jf.getInputStream(e));
        }
    }

    /** the depends elements; a package's dependency on itself (BEAST.app 2.8.0 has one) is ignored */
    private List<Dependency> dependencies(Document doc, String self) {
        List<Dependency> result = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("depends");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element d = (Element) nodes.item(i);
            String on = d.getAttribute("on");
            if (on.equals(self)) {
                warnings.add(self + ": its version.xml declares a dependency on " + self + " itself; it is ignored");
                continue;
            }
            String atLeast = d.getAttribute("atleast"), atMost = d.getAttribute("atmost");
            result.add(new Dependency(on, atLeast.isEmpty() ? null : Version.of(atLeast),
                    atMost.isEmpty() ? null : Version.of(atMost)));
        }
        return result;
    }

    /** the service providers of a version.xml: {@code <service type><provider classname/>} */
    static Map<String, Set<String>> services(Document doc) {
        Map<String, Set<String>> services = new java.util.TreeMap<>();
        NodeList nodes = doc.getElementsByTagName("service");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element service = (Element) nodes.item(i);
            for (Node n = service.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element p && p.getTagName().equals("provider")) {
                    services.computeIfAbsent(service.getAttribute("type"), k -> new java.util.TreeSet<>())
                            .add(p.getAttribute("classname"));
                }
            }
        }
        return services;
    }

    /**
     * Services are module-info's job (decision 5): warns about providers that only version.xml
     * declares, and about providers that are not in the package's own jars at all.
     */
    private void checkServices(String name, Version version, Map<String, Set<String>> declared, List<Path> ownJars) {
        if (declared.isEmpty()) {
            return;
        }
        Map<String, Set<String>> provided = new HashMap<>();
        Set<String> classes = new java.util.HashSet<>();
        for (Path jar : ownJars) {
            try (JarFile jf = new JarFile(jar.toFile())) {
                ModuleFinder.of(jar).findAll().forEach(ref -> ref.descriptor().provides().forEach(p ->
                        provided.computeIfAbsent(p.service(), k -> new java.util.HashSet<>()).addAll(p.providers())));
                jf.stream().map(JarEntry::getName).filter(n -> n.endsWith(".class"))
                        .forEach(n -> classes.add(n.substring(0, n.length() - 6).replace('/', '.')));
            } catch (IOException | RuntimeException e) {
                // unreadable jars are reported by readJar
            }
        }
        List<String> onlyInVersionXml = new ArrayList<>(), notInJars = new ArrayList<>();
        declared.forEach((service, providers) -> providers.forEach(p -> {
            if (!classes.contains(p) && !classes.contains(p.replace('$', '.'))) {
                notInJars.add(p);
            } else if (!provided.getOrDefault(service, Set.of()).contains(p)) {
                onlyInVersionXml.add(service + " -> " + p);
            }
        }));
        if (!onlyInVersionXml.isEmpty()) {
            warnings.add(name + " " + version + ": " + onlyInVersionXml.size() + " service provider(s) declared only in"
                    + " version.xml, e.g. " + onlyInVersionXml.get(0) + "; declare them with 'provides' in module-info,"
                    + " which is what counts");
        }
        if (!notInJars.isEmpty()) {
            warnings.add(name + " " + version + ": its version.xml lists " + notInJars.size()
                    + " service provider(s) that are not in its jars, e.g. " + notInJars.get(0));
        }
    }

    private static String text(Element parent, String tag) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.getTagName().equals(tag)) {
                return e.getTextContent().trim();
            }
        }
        return null;
    }

    private static Document parse(InputStream in) throws Exception {
        try (in) {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = f.newDocumentBuilder().parse(in);
            doc.normalize();
            return doc;
        }
    }

}
