package beast.base.inference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import beast.base.parser.XMLParser;
import beast.base.util.Randomizer;

/**
 * MCMC.requestStop(): the BEAST dialog's Stop button (#328). Thread.stop() throws since
 * Java 20 and is gone in Java 26, so the chain has to stop itself.
 */
class MCMCStopTest {

    /** beast-base/examples/testHKY.xml (5,000,000 samples); tests run in beast-base/target/test-classes. */
    private static final File TEST_HKY = findTestHKY();

    private static File findTestHKY() {
        for (File d = new File(System.getProperty("user.dir")).getAbsoluteFile(); d != null; d = d.getParentFile()) {
            for (String rel : new String[] {"examples/testHKY.xml", "beast-base/examples/testHKY.xml"}) {
                File f = new File(d, rel);
                if (f.isFile()) {
                    return f;
                }
            }
        }
        throw new IllegalStateException("beast-base/examples/testHKY.xml not found above " + System.getProperty("user.dir"));
    }

    @TempDir
    Path dir;

    @AfterEach
    void reset() {
        System.clearProperty("file.name.prefix");
        Logger.FILE_MODE = Logger.LogFileMode.only_new;
    }

    /** Runs testHKY on another thread, asks it to stop after a while, and returns the stopped chain. */
    private MCMC runAndStop(boolean resume) throws Exception {
        System.setProperty("file.name.prefix", dir + File.separator);
        Logger.FILE_MODE = resume ? Logger.LogFileMode.resume : Logger.LogFileMode.overwrite;
        Randomizer.setSeed(1);
        // fixed log names: $(seed) in them differs between the two runs
        Path xml = dir.resolve("testHKY.xml");
        Files.writeString(xml, Files.readString(TEST_HKY.toPath()).replace("test.$(seed).", "test."));
        MCMC mcmc = (MCMC) new XMLParser().parseFile(xml.toFile());
        mcmc.setStateFile("testHKY.xml.state", resume); // file.name.prefix puts it in dir

        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread chain = new Thread(() -> {
            try {
                mcmc.run();
            } catch (Throwable e) {
                failure.set(e);
            }
        });
        chain.start();
        Thread.sleep(1500);
        assertTrue(mcmc.requestStop(), "a plain MCMC can be stopped");
        chain.join(60_000);
        assertFalse(chain.isAlive(), "the chain ends soon after the request");
        assertNull(failure.get());
        return mcmc;
    }

    private static long stateFileSample(Path stateFile) throws Exception {
        Matcher m = Pattern.compile("sample='(\\d+)'").matcher(Files.readString(stateFile));
        assertTrue(m.find(), "state file names its sample");
        return Long.parseLong(m.group(1));
    }

    /** Sample numbers of the data rows of a trace log. */
    private static List<Long> logSamples(Path log) throws Exception {
        return Files.readAllLines(log).stream()
                .filter(l -> !l.isEmpty() && Character.isDigit(l.charAt(0)))
                .map(l -> Long.parseLong(l.substring(0, l.indexOf('\t'))))
                .toList();
    }

    @Test
    void stopStoresStateAndResumeContinues() throws Exception {
        MCMC first = runAndStop(false);
        long stopped = first.stoppedAtSample;
        assertTrue(stopped > 0 && stopped < 5_000_000, "stopped part way: " + stopped);

        Path state = dir.resolve("testHKY.xml.state");
        assertEquals(stopped, stateFileSample(state), "state file holds the stopped sample");

        Path log = dir.resolve("test.log");
        List<Long> before = logSamples(log);
        assertTrue(before.get(before.size() - 1) <= stopped, "no log row after the stopped sample");

        MCMC second = runAndStop(true);
        List<Long> after = logSamples(log);
        assertTrue(after.size() > before.size(), "the resumed run appends to the log");
        for (int i = 1; i < after.size(); i++) {
            assertTrue(after.get(i) > after.get(i - 1), "log samples keep increasing at row " + i);
        }
        assertTrue(second.stoppedAtSample > 0);
    }

    @Test
    void subclassReplacingDoLoopIsNotStoppable() {
        MCMC replaced = new MCMC() {
            @Override
            protected void doLoop() {
            }
        };
        assertFalse(replaced.requestStop());

        MCMC optedIn = new MCMC() {
            @Override
            protected void doLoop() {
            }

            @Override
            protected boolean checksStopRequests() {
                return true;
            }
        };
        assertTrue(optedIn.requestStop());
        assertTrue(optedIn.isStopRequested());

        assertTrue(new MCMC().requestStop(), "MCMC's own loop checks for stop requests");
    }
}
