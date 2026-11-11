package test.beast.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The exit status of BEAST tells scripts and cluster jobs whether the run finished.
 * BEAST is started in a separate JVM, since the exit status is what is tested (issue #265).
 */
public class BeastMainExitStatusTest {

    /** %s is the start value of x; its prior is Uniform(0,1) */
    private static final String XML = """
            <?xml version='1.0' encoding='UTF-8'?>
            <beast version="2.8" namespace="beast.base.inference:beast.base.inference.parameter:beast.base.inference.distribution">
                <run spec="MCMC" id="mcmc" chainLength="1000">
                    <state>
                        <stateNode id="x" spec="RealParameter" value="%s"/>
                    </state>
                    <distribution spec="CompoundDistribution" id="posterior">
                        <distribution id="x.prior" spec="Prior" x="@x">
                            <distr spec="Uniform" lower="0.0" upper="1.0"/>
                        </distribution>
                    </distribution>
                    <operator id="x.scale" spec="beast.base.evolution.operator.ScaleOperator" scaleFactor="0.75" weight="1" parameter="@x"/>
                    <logger id="screenlog" spec="Logger" logEvery="100">
                        <log idref="posterior"/>
                    </logger>
                </run>
            </beast>
            """;

    @TempDir
    Path dir;

    private String output;

    /** run BEAST on the XML in a new JVM, and return its exit status */
    private int runBeast(String xml) throws Exception {
        Path file = dir.resolve("test.xml");
        Files.writeString(file, xml, StandardCharsets.UTF_8);

        // everything the test JVM has on its class path and module path goes on the class path
        String classPath = System.getProperty("java.class.path", "");
        String modulePath = System.getProperty("jdk.module.path");
        if (modulePath != null && !modulePath.isEmpty()) {
            classPath = classPath.isEmpty() ? modulePath : classPath + File.pathSeparator + modulePath;
        }

        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-cp");
        command.add(classPath);
        // do not load the packages installed for the user who runs the test
        command.add("-Dbeast.user.package.dir=" + dir.resolve("packages"));
        String packagePath = System.getProperty("BEAST_PACKAGE_PATH");
        if (packagePath != null) {
            command.add("-DBEAST_PACKAGE_PATH=" + packagePath);
        }
        command.add("-Djava.awt.headless=true");
        command.add("beast.base.minimal.BeastMain");
        command.add("-overwrite");
        command.add(file.toString());

        File log = dir.resolve("beast.out").toFile();
        Process process = new ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(log)
                .start();
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        output = Files.readString(log.toPath());
        assertTrue(finished, "BEAST did not finish within 120 seconds:\n" + output);
        return process.exitValue();
    }

    @Test
    public void runThatFinishesExitsWithZero() throws Exception {
        int status = runBeast(XML.formatted("0.5"));
        assertEquals(0, status, output);
        assertTrue(output.contains("End likelihood"), output);
    }

    @Test
    public void exceptionInTheRunExitsWithNonZero() throws Exception {
        // x starts outside its prior, so the XML parses but MCMC.run() throws a RuntimeException
        int status = runBeast(XML.formatted("5.0"));
        assertTrue(output.contains("Could not find a proper state to initialise"),
                "the run should fail in MCMC.run(), but the output is:\n" + output);
        assertNotEquals(0, status, "BEAST exits with status 0 after an exception in the run:\n" + output);
    }

    @Test
    public void xmlThatDoesNotParseExitsWithNonZero() throws Exception {
        int status = runBeast(XML.formatted("0.5").replace("spec=\"Prior\"", "spec=\"NoSuchClass\""));
        assertNotEquals(0, status, output);
    }
}
