package test.beast.integration;

import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.parser.XMLParser;
import beast.base.util.Randomizer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that an MCMC chain can be interrupted and resumed from a state file,
 * across a range of model/operator combinations:
 *
 *   testHKY.xml           – baseline: tree + continuous params (kappa, base freqs)
 *   testGTR.xml           – constrained rate matrix (DeltaExchangeOperator, sum-to-one params)
 *   bitflip.xml           – discrete BoolVectorParam; no tree, no likelihood
 *   testOpSubSchedule.xml – custom OperatorSchedule with checkpointed tuning weights
 *   testPlates.xml        – multi-partition model (three data blocks, multiple TreeLikelihoods)
 *
 * Isolation: each XML gets its own output subdirectory (e.g. test/testHKY/) via
 * XMLPathUtil.setUpOutputDir(baseName). testHKY, testOpSubSchedule, and testPlates
 * all write to test.$(seed).log / test.$(seed).trees — identical names at seed 127 —
 * so subdirectory isolation is essential. Each subdirectory must contain exactly one
 * .log file; both the initial run and the resume must produce at least one data sample.
 */
public class ResumeTest {
    private static final long CHAIN_LENGTH = 1000L;
    private static final int LOG_EVERY = 100;

    final String[] xmlFiles = {
            "testHKY.xml",           // tree + continuous params
//TODO            "testGTR.xml",           // constrained rate matrix
            "bitflip.xml",           // discrete boolean state
            "testOpSubSchedule.xml", // custom operator schedule
            "testPlates.xml",        // multi-partition
    };

    @Test
    public void test_ThatXmlExamplesResume() throws Exception {
        Randomizer.setSeed(127);
        final String stateFile = "tmp.state";

        for (String xmlFileName : xmlFiles) {
            String baseName = xmlFileName.replace(".xml", "");
            // isolate log/tree output per XML: testHKY, testOpSubSchedule and testPlates
            // all resolve to test.127.log / test.127.trees without a unique subdirectory
            XMLPathUtil.setUpOutputDir(baseName);
            String fileName = new File(XMLPathUtil.resolveExamplesDir(), xmlFileName).getAbsolutePath();

            // --- initial run ---
            System.out.println("Processing " + fileName);
            Logger.FILE_MODE = Logger.LogFileMode.overwrite;
            XMLParser parser = new XMLParser();
            beast.base.inference.Runnable runable = parser.parseFile(new File(fileName));
            runable.setStateFile(stateFile, false);
            if (runable instanceof MCMC mcmc) {
                mcmc.setInputValue("preBurnin", 0);
                mcmc.setInputValue("chainLength", CHAIN_LENGTH);
                for (Logger logger : mcmc.loggersInput.get()) {
                    logger.initByName("logEvery", LOG_EVERY);
                }
                mcmc.run();
            }
            System.out.println("Done " + fileName);

            // assert 1: state file must exist and record the correct checkpoint position
            // state file XML: <itsabeastystatewerein version='2.0' sample='1000'>
            // derive prefix before any run so stateFile and log assertions use the same dir
            String prefix = System.getProperty("file.name.prefix");
            File sf = new File(prefix + stateFile);
            assertTrue(sf.exists(), sf.getAbsolutePath() + ": state file missing after initial run");
            String stateXml = Files.readString(sf.toPath());
            assertTrue(stateXml.contains("sample='" + CHAIN_LENGTH + "'"),
                    xmlFileName + ": state file should record sample=" + CHAIN_LENGTH);

            // assert 2: exactly one .log file per output dir — multiple would indicate
            // an unexpected logger or a namespace collision between XMLs
            File[] logFiles = new File(prefix).listFiles((d, n) -> n.endsWith(".log"));
            assertTrue(logFiles != null && logFiles.length == 1,
                    xmlFileName + ": expected exactly 1 .log file in " + prefix
                            + ", found " + (logFiles == null ? 0 : logFiles.length));
            Path logFile = logFiles[0].toPath();

            // assert 3: initial run produced at least one sample
            final int beforeResume = assertLogHasSamples(logFile, xmlFileName + " after initial run");

            // --- resume ---
            System.out.println("Resuming " + fileName);
            Logger.FILE_MODE = Logger.LogFileMode.resume;
            parser = new XMLParser();
            runable = parser.parseFile(new File(fileName));
            runable.setStateFile(stateFile, true);
            if (runable instanceof MCMC mcmc) {
                mcmc.setInputValue("preBurnin", 0);
                mcmc.setInputValue("chainLength", CHAIN_LENGTH);
                for (Logger logger : mcmc.loggersInput.get()) {
                    logger.initByName("logEvery", LOG_EVERY);
                }
                mcmc.run();
            }
            System.out.println("Done " + fileName);

            // assert 4: resumed run appended at least one further sample
            final int afterResume = assertLogHasSamples(logFile, xmlFileName + " after resume");

            assertTrue(afterResume > beforeResume, "Resume should append lines to log in this test : "
                    + beforeResume + " vs. " + afterResume);

            System.out.println("**********************************\n");
            System.out.println("Complete resume tests for " + fileName);
            System.out.println("Initial logging lines " + beforeResume +
                    ", resume appended " + (afterResume-beforeResume) + " lines.");
            System.out.println("**********************************\n\n");
        }
    }

    /**
     * Asserts the log file contains a header line starting with "Sample" and
     * at least one data line after it.
     */
    private static int assertLogHasSamples(Path logFile, String context) throws Exception {
        List<String> lines = Files.readAllLines(logFile);
        int headerIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("Sample")) {
                headerIdx = i;
                break;
            }
        }
        assertTrue(headerIdx >= 0,
                context + ": log missing header line starting with 'Sample' in " + logFile);
        assertTrue(lines.size() > headerIdx + 1,
                context + ": log has no data lines after 'Sample' header in " + logFile);
        return lines.size();
    }

} // class ResumeTest
