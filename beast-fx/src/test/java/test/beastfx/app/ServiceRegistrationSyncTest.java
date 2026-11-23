package test.beastfx.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The same check as beast-base's ServiceRegistrationSyncTest, for beast-fx (beast-fx's tests
 * cannot see beast-base's). BEAST finds service providers in two places: {@code provides} in module-info, which counts
 * only on the module path, and {@code version.xml}, which is all there is on the classpath
 * (e.g. a tool that runs {@code java -cp lib/*}). A provider listed only in module-info cannot
 * be created from XML on the classpath, as happened to beast-base's {@code VectorElement}.
 * <p>
 * The other direction is allowed: version.xml may list classes that module-info cannot, such
 * as abstract classes, non-static inner classes and classes without a no-arg constructor.
 */
public class ServiceRegistrationSyncTest {

    @Test
    void everyModuleProviderIsInVersionXml() throws Exception {
        Path module = moduleDir();
        Map<String, Set<String>> missing = missingFromVersionXml(
                module.resolve("src/main/java/module-info.java"), module.resolve("version.xml"));
        assertTrue(missing.isEmpty(), "listed in beast-fx's module-info but not in version.xml: " + missing);
    }

    /** the module's folder: tests run in target/test-classes, so look upwards from there */
    static Path moduleDir() {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("src/main/java/module-info.java"))) {
                return p;
            }
        }
        throw new IllegalStateException("no module-info.java above " + System.getProperty("user.dir"));
    }

    /** service type to the providers module-info declares but version.xml does not list */
    public static Map<String, Set<String>> missingFromVersionXml(Path moduleInfo, Path versionXml) throws Exception {
        Map<String, Set<String>> declared = moduleProviders(moduleInfo);
        Map<String, Set<String>> listed = versionXmlProviders(versionXml);
        Map<String, Set<String>> missing = new TreeMap<>();
        declared.forEach((type, providers) -> {
            Set<String> absent = new TreeSet<>(providers);
            absent.removeAll(listed.getOrDefault(type, Set.of()));
            if (!absent.isEmpty()) {
                missing.put(type, absent);
            }
        });
        return missing;
    }

    private static final Pattern PROVIDES = Pattern.compile("provides\\s+([\\w.]+)\\s+with([^;]*);");

    static Map<String, Set<String>> moduleProviders(Path moduleInfo) throws IOException {
        String source = Files.readString(moduleInfo)
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("//[^\n]*", "");
        Map<String, Set<String>> providers = new TreeMap<>();
        Matcher m = PROVIDES.matcher(source);
        while (m.find()) {
            Set<String> set = providers.computeIfAbsent(m.group(1), k -> new TreeSet<>());
            for (String name : m.group(2).split(",")) {
                if (!name.isBlank()) {
                    set.add(name.strip());
                }
            }
        }
        return providers;
    }

    /** nested classes as module-info writes them: {@code Outer.Inner}, not {@code Outer$Inner} */
    static Map<String, Set<String>> versionXmlProviders(Path versionXml) throws Exception {
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(versionXml.toFile()).getDocumentElement();
        Map<String, Set<String>> providers = new TreeMap<>();
        NodeList services = root.getElementsByTagName("service");
        for (int i = 0; i < services.getLength(); i++) {
            Element service = (Element) services.item(i);
            Set<String> set = providers.computeIfAbsent(service.getAttribute("type"), k -> new TreeSet<>());
            NodeList list = service.getElementsByTagName("provider");
            for (int j = 0; j < list.getLength(); j++) {
                set.add(((Element) list.item(j)).getAttribute("classname").replace('$', '.'));
            }
        }
        return providers;
    }
}
