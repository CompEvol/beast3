package test.beast.integration;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.parser.JSONParser;
import beast.base.parser.XMLParser;
import beast.base.util.Randomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checks that the release examples (the XML and JSON files directly under
 * {@code beast.base/examples}) build no objects of deprecated classes.
 *
 * <p>Each file is parsed and every BEAST object reachable through the inputs of the
 * run is checked for {@link Deprecated} on its class or an enclosing class. Parsing,
 * rather than searching the file text, resolves element names, namespaces and
 * {@code <map>} entries to the classes BEAST actually builds. Objects the parser builds
 * but the run never reaches are not checked.
 *
 * <p>The legacy BEAST 2 examples under {@code examples/legacy} are not checked.
 */
// setUp sets file.name.prefix, a JVM-wide global; see ExampleXmlParsingTest.
@ResourceLock(value = "beast.logger.globals", mode = ResourceAccessMode.READ_WRITE)
public class ExampleDeprecationTest {

    @BeforeEach
    void setUp() { XMLPathUtil.setUpOutputDir(); }

    @Test
    public void testExamplesUseNoDeprecatedClasses() throws Exception {
        File dir = new File(XMLPathUtil.resolveExamplesDir());
        assertTrue(dir.isDirectory(), "Example directory does not exist: " + dir);
        String[] files = dir.list((d, name) -> name.endsWith(".xml") || name.endsWith(".json"));
        assertTrue(files != null && files.length > 0, "No examples found in " + dir);
        Arrays.sort(files);

        List<String> problems = new ArrayList<>();
        for (String name : files) {
            Randomizer.setSeedReproducible(127);
            File file = new File(dir, name);
            Object run;
            try {
                run = name.endsWith(".json") ? new JSONParser().parseFile(file) : new XMLParser().parseFile(file);
            } catch (Exception e) {
                // Parse failures are reported by ExampleXmlParsingTest and ExampleJSONParsingTest.
                continue;
            }
            Set<String> deprecated = deprecatedClasses(run);
            if (!deprecated.isEmpty()) {
                problems.add(name + ": " + String.join(", ", deprecated));
            }
        }
        if (!problems.isEmpty()) {
            fail("Examples use deprecated classes:\n  " + String.join("\n  ", problems));
        }
    }

    /** Names of the deprecated classes of all BEAST objects reachable from {@code root}. */
    static Set<String> deprecatedClasses(Object root) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        collect(root, seen);
        Set<String> deprecated = new TreeSet<>();
        for (Object o : seen) {
            if (isDeprecated(o.getClass())) {
                deprecated.add(o.getClass().getName());
            }
        }
        return deprecated;
    }

    private static void collect(Object o, Set<Object> seen) {
        if (o instanceof Collection<?> c) {
            for (Object x : c) collect(x, seen);
        } else if (o instanceof Map<?, ?> m) {
            for (Object x : m.values()) collect(x, seen);
        } else if (o instanceof BEASTInterface b && seen.add(b)) {
            for (Input<?> input : b.listInputs()) collect(input.get(), seen);
        }
    }

    private static boolean isDeprecated(Class<?> c) {
        for (Class<?> k = c; k != null; k = k.getEnclosingClass()) {
            if (k.isAnnotationPresent(Deprecated.class)) return true;
        }
        return false;
    }
}
