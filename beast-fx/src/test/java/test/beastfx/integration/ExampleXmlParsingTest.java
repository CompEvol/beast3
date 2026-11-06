package test.beastfx.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.parser.XMLParser;
import beast.base.util.Randomizer;

/**
 * Checks the BEAST 3 examples that need beast-fx classes ({@code beast.fx/examples}, released as
 * {@code examples/beastfx/}), as beast-base's {@code ExampleXmlParsingTest} does for
 * {@code beast.base/examples}: every XML file directly in the folder must parse, and an MCMC must run
 * 1000 steps.
 */
// Logger.FILE_MODE and file.name.prefix are JVM-wide globals: serialize the tests that set them.
@ResourceLock(value = "beast.logger.globals", mode = ResourceAccessMode.READ_WRITE)
public class ExampleXmlParsingTest {

    /** Folder of beast-fx examples on the test classpath. */
    static File examplesDir(String sub) {
        URL url = ExampleXmlParsingTest.class.getClassLoader().getResource("beast.fx/examples");
        assertTrue(url != null, "beast.fx/examples not found on the test classpath");
        try {
            File dir = new File(url.toURI());
            return sub == null ? dir : new File(dir, sub);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /** XML files directly in dir, sorted. */
    static String[] xmlFiles(File dir) {
        assertTrue(dir.isDirectory(), "Example directory does not exist: " + dir);
        String[] files = dir.list((d, name) -> name.endsWith(".xml"));
        assertTrue(files != null && files.length > 0, "No examples found in " + dir);
        Arrays.sort(files);
        return files;
    }

    /** Write log and tree files to ./test/, as beast-base's integration tests do. */
    static void setUpOutputDir() {
        File out = new File("test");
        if (!out.exists()) {
            out.mkdirs();
        }
        System.setProperty("file.name.prefix", "test/");
        Logger.FILE_MODE = Logger.LogFileMode.overwrite;
    }

    @BeforeEach
    void setUp() { setUpOutputDir(); }

    @Test
    public void testExamplesParse() {
        File dir = examplesDir(null);
        List<String> failed = new ArrayList<>();
        for (String name : xmlFiles(dir)) {
            Randomizer.setSeedReproducible(127);
            System.out.println("Processing " + name);
            try {
                new XMLParser().parseFile(new File(dir, name));
            } catch (Exception e) {
                System.out.println("ExampleXmlParsing::Failed for " + name + ": " + e.getMessage());
                failed.add(name);
            }
        }
        assertTrue(failed.isEmpty(), "beast-fx examples that do not parse: " + failed);
    }

    @Test
    public void testExamplesRun() {
        File dir = examplesDir(null);
        List<String> failed = new ArrayList<>();
        int seed = 127;
        for (String name : xmlFiles(dir)) {
            Randomizer.setSeedReproducible(seed);
            seed += 10;
            System.out.println("Running " + name);
            try {
                beast.base.inference.Runnable runnable = new XMLParser().parseFile(new File(dir, name));
                if (runnable instanceof MCMC mcmc) {
                    mcmc.setInputValue("preBurnin", 0);
                    mcmc.setInputValue("chainLength", 1000L);
                    mcmc.run();
                }
            } catch (Exception e) {
                System.out.println("ExampleXmlParsing::Failed for " + name + ": " + e.getMessage());
                failed.add(name);
            }
        }
        assertTrue(failed.isEmpty(), "beast-fx examples that do not run: " + failed);
    }

    /**
     * The BEAST 2 examples that need beast-fx classes ({@code beast.fx/examples/legacy}, e.g. seqgen)
     * are not converted: they must still parse in BEAST 2.8 (BEAST 3). As beast-base's
     * {@code ExampleXmlParsingTest.test_ThatLegacyXmlExamplesParse} does for
     * {@code beast.base/examples/legacy}.
     */
    @Test
    public void testLegacyExamplesParse() {
        File dir = examplesDir("legacy");
        List<String> failed = new ArrayList<>();
        for (String name : xmlFiles(dir)) {
            Randomizer.setSeedReproducible(127);
            System.out.println("Processing legacy " + name);
            try {
                new XMLParser().parseFile(new File(dir, name));
            } catch (Exception e) {
                System.out.println("ExampleXmlParsing::Failed for legacy " + name + ": " + e.getMessage());
                failed.add(name);
            }
        }
        assertTrue(failed.isEmpty(), "Legacy BEAST 2 beast-fx examples that no longer parse: " + failed);
    }
}
