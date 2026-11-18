package test.beast.integration;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.spec.beast2vs1.OneOnX;
import beast.pkgmgmt.BEASTClassLoader;
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
 * <p>It also checks that no tensor input is given a value of the wrong domain (#226), in
 * the release examples and in the test XML in {@code examples/beast2vs1}.
 *
 * <p>The legacy BEAST 2 examples under {@code examples/legacy} are not checked.
 */
// setUp sets file.name.prefix, a JVM-wide global; see ExampleXmlParsingTest.
@ResourceLock(value = "beast.logger.globals", mode = ResourceAccessMode.READ_WRITE)
public class ExampleDeprecationTest {

    @BeforeEach
    void setUp() { XMLPathUtil.setUpOutputDir(); }

    /**
     * Each release example builds no objects of deprecated classes. Each release example,
     * and each test XML in {@code examples/beast2vs1}, gives no tensor input a value whose
     * domain does not fit the declared one, e.g. a parameter with domain Real for an input
     * declared PositiveReal. That is deprecated (#226) and will be an error in a future
     * version. The beast2vs1 files are test fixtures that use some deprecated classes on
     * purpose, so only their domains are checked. A file that does not parse fails the test.
     */
    @Test
    public void testExamplesUseNoDeprecatedClassesOrDomains() throws Exception {
        File dir = new File(XMLPathUtil.resolveExamplesDir());
        assertTrue(dir.isDirectory(), "Example directory does not exist: " + dir);
        List<File> files = new ArrayList<>();
        for (File d : new File[] {dir, new File(dir, "beast2vs1")}) {
            // testSRD06CP12_3.xml is all commented out: its test in SubstitutionModelTest is dropped
            File[] list = d.listFiles((x, name) -> (name.endsWith(".xml") || name.endsWith(".json"))
                    && !name.equals("testSRD06CP12_3.xml"));
            assertTrue(list != null && list.length > 0, "No examples found in " + d);
            files.addAll(Arrays.asList(list));
        }
        files.sort(null);
        // the beast2vs1 test XML uses this test-only distribution, as TestFramework does
        BEASTClassLoader.addService(BEASTInterface.class.getName(), OneOnX.class.getName(), "BEAST.base");

        List<String> problems = new ArrayList<>();
        for (File file : files) {
            String name = dir.toPath().relativize(file.toPath()).toString();
            boolean release = file.getParentFile().equals(dir);
            Randomizer.setSeedReproducible(127);
            Object run;
            try {
                run = name.endsWith(".json") ? new JSONParser().parseFile(file) : new XMLParser().parseFile(file);
            } catch (Exception e) {
                problems.add(name + ": does not parse: " + e.getMessage());
                continue;
            }
            Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            collect(run, seen);
            Set<String> deprecated = new TreeSet<>();
            Set<String> mismatches = new TreeSet<>();
            for (Object o : seen) {
                if (release && isDeprecated(o.getClass())) {
                    deprecated.add(o.getClass().getName());
                }
                BEASTInterface b = (BEASTInterface) o;
                for (Input<?> input : b.listInputs()) {
                    String mismatch = input.domainMismatch(b);
                    if (mismatch != null) {
                        mismatches.add(mismatch);
                    }
                }
            }
            if (!deprecated.isEmpty()) {
                problems.add(name + ": deprecated classes " + String.join(", ", deprecated));
            }
            for (String mismatch : mismatches) {
                problems.add(name + ": " + mismatch);
            }
        }
        if (!problems.isEmpty()) {
            fail("Problems in examples:\n  " + String.join("\n  ", problems));
        }
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
