package test.beast.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import beast.base.inference.Logger;
import beast.base.inference.parameter.RealParameter;

import java.io.*;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
/**
 * junit 4
 * http://www.asjava.com/junit/junit-3-vs-junit-4-comparison/
 * http://stackoverflow.com/questions/2469911/how-do-i-assert-my-exception-message-with-junit-test-annotation
 *
 * @author Walter Xie
 */
public class LoggerTest {
    Logger logger;

    /**
     * Logger.sampleOffset is protected static, so it is not reachable from this package
     * directly; a subclass can reach it because protected static members are inherited
     * regardless of package.
     */
    private static class LoggerGlobals extends Logger {
        static void reset() {
            Logger.sampleOffset = -1;
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        // file.name.prefix, Logger.FILE_MODE and Logger.sampleOffset are JVM-wide globals
        // that the integration tests set and do not restore (see XMLPathUtil.setUpOutputDir
        // and ResumeTest). Whether this class runs before or after them varies between
        // surefire runs, which made testFileLog fail intermittently: a stale prefix sends
        // the log to <prefix>beast.log while the assertions resolve "beast.log" against the
        // working directory, and a stale sampleOffset is added to every logged sample.
        System.clearProperty("file.name.prefix");
        Logger.FILE_MODE = Logger.LogFileMode.only_new;
        LoggerGlobals.reset();
        logger = new Logger();
    }

    @Test
    public void isLoggingToStdout() throws Exception {
        logger = new Logger();
        logger.initByName("fileName", null, "log", new RealParameter());
        assertTrue(logger.isLoggingToStdout(), "fileName == null");

        logger = new Logger();
        logger.initByName("fileName", "", "log", new RealParameter());
        assertTrue(logger.isLoggingToStdout(), "fileName.length() == 0");

        logger = new Logger();
        logger.initByName("fileName", "beast.log", "log", new RealParameter());
        assertFalse(logger.isLoggingToStdout(), "fileName = \"beast.log\"");
    }

    @Test
    public void testFileLog() throws Exception {
        logger = new Logger();
        logger.initByName("fileName", "beast.log", "log", new RealParameter(new Double[]{0.3, 0.7}));
        File f_log = new File(logger.fileNameInput.get());
        if (f_log.exists()) {
            boolean log_deleted = f_log.delete();
            System.out.println("Delete log : " + f_log.getAbsolutePath() + " for testFileLog.");
        }

        logger.init();
        assertTrue(f_log.exists(), "beast.log created successfully");

        // rI >= 0
        int rI = new Random().nextInt(10000000);
        logger.log(-1);
        logger.log(rI);
        logger.close();

        //TODO cannot get "closing" status from PrintStream
//        assertNull("m_out is beast.log after close", logger.getM_out());

        BufferedReader in = new BufferedReader(new FileReader(f_log));
        // column names
        String line = in.readLine();
        // 1st sample
        String sample1 = in.readLine();
        String[] sp = sample1.split("\t", -1);
        assertFalse(sp[0].equals("-1"), "check beast.log -1 not logged"); 
        assertEquals(Integer.toString(rI), sp[0], "check beast.log 1st sample");
    }

    @Test
    public void testScreenLog() throws Exception {
        logger = new Logger();
        logger.initByName("fileName", "", "log", new RealParameter(new Double[]{0.3, 0.7}));

        logger.init();
        assertTrue(logger.getM_out() == System.out, "m_out is System.out");

        logger.log(1);
        //TODO cannot extract content

        // close all file, except stdout
        logger.close();
        assertTrue(logger.getM_out() == System.out, "m_out is still System.out after close");
    }

}