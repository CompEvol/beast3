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

    /** testHKY in dir, without pre-burnin, so any proposal is a sample */
    private MCMC parse(boolean resume) throws Exception {
        System.setProperty("file.name.prefix", dir + File.separator);
        Logger.FILE_MODE = resume ? Logger.LogFileMode.resume : Logger.LogFileMode.overwrite;
        Randomizer.setSeed(1);
        // fixed log names: $(seed) in them differs between the two runs
        Path xml = dir.resolve("testHKY.xml");
        Files.writeString(xml, Files.readString(TEST_HKY.toPath())
                .replace("test.$(seed).", "test.")
                .replace("preBurnin=\"50000\"", "preBurnin=\"0\""));
        MCMC mcmc = (MCMC) new XMLParser().parseFile(xml.toFile());
        mcmc.setStateFile("testHKY.xml.state", resume); // file.name.prefix puts it in dir
        return mcmc;
    }

    /** Proposals made by the chain's operators so far, including those restored by -resume. */
    private static long proposals(MCMC mcmc) {
        long n = 0;
        for (Operator operator : mcmc.operatorsInput.get()) {
            n += operator.get_m_nNrAccepted() + operator.get_m_nNrRejected();
        }
        return n;
    }

    /**
     * Runs testHKY on another thread, asks it to stop once it has made 20,000 proposals more
     * than at the start, and returns the stopped chain.
     */
    private MCMC runAndStop(boolean resume, long proposalsAtStart) throws Exception {
        MCMC mcmc = parse(resume);

        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread chain = new Thread(() -> {
            try {
                mcmc.run();
            } catch (Throwable e) {
                failure.set(e);
            }
        });
        chain.start();
        // wait until the chain is sampling, rather than for a fixed time (#329 review)
        long deadline = System.currentTimeMillis() + 60_000;
        while (proposals(mcmc) < proposalsAtStart + 20_000 && chain.isAlive()
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(chain.isAlive(), "the chain is still running when asked to stop");
        assertTrue(mcmc.requestStop(), "a plain MCMC can be stopped");
        chain.join(60_000);
        assertFalse(chain.isAlive(), "the chain ends soon after the request");
        assertNull(failure.get());
        return mcmc;
    }

    private static long stateFileSample(Path stateFile) throws Exception {
        Matcher m = Pattern.compile("sample='(-?\\d+)'").matcher(Files.readString(stateFile));
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
        MCMC first = runAndStop(false, 0);
        long stopped = first.stoppedAtSample;
        assertTrue(stopped > 0 && stopped < 5_000_000, "stopped part way: " + stopped);

        Path state = dir.resolve("testHKY.xml.state");
        assertEquals(stopped, stateFileSample(state), "state file holds the stopped sample");

        Path log = dir.resolve("test.log");
        List<Long> before = logSamples(log);
        assertTrue(before.get(before.size() - 1) <= stopped, "no log row after the stopped sample");

        MCMC second = runAndStop(true, proposals(first));
        List<Long> after = logSamples(log);
        assertTrue(after.size() > before.size(), "the resumed run appends to the log");
        for (int i = 1; i < after.size(); i++) {
            assertTrue(after.get(i) > after.get(i - 1), "log samples keep increasing at row " + i);
        }
        assertTrue(second.stoppedAtSample > 0);
    }

    @Test
    void stopBeforeTheFirstSampleSaysSo() throws Exception {
        MCMC mcmc = parse(false);
        assertTrue(mcmc.requestStop());
        mcmc.run();
        assertEquals(-mcmc.burnIn - 1, mcmc.stoppedAtSample);
        assertTrue(mcmc.stoppedMessage().startsWith("Stopped on request before the first sample"), mcmc.stoppedMessage());
        assertEquals(mcmc.stoppedAtSample, stateFileSample(dir.resolve("testHKY.xml.state")));
    }

    @Test
    void stoppedMessages() {
        MCMC mcmc = new MCMC();
        mcmc.burnIn = 100;
        mcmc.chainLength = 1000;
        mcmc.stoppedAtSample = -61; // samples -100 to -61 done
        assertEquals("Stopped on request during pre-burnin, after 40 of 100 pre-burnin samples; the state is stored; "
                + "-resume starts a new chain of 1000 samples from it, without the rest of the pre-burnin, "
                + "appending to the logs", mcmc.stoppedMessage());
        mcmc.stoppedAtSample = 250;
        assertEquals("Stopped on request at sample 250; the state is stored; "
                + "-resume starts a new chain of 1000 samples from it, appending to the logs", mcmc.stoppedMessage());
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

        MCMC ownRun = new MCMC() {
            @Override
            public void run() {
            }
        };
        assertFalse(ownRun.requestStop(), "its run() may overwrite the stopped state");
    }
}
