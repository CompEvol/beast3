package beast.base.spec.beast2vs1;

import beagle.BeagleFlag;
import beast.base.core.BEASTInterface;
import beast.base.inference.Logger;
import beast.base.parser.XMLParser;
import beast.base.trace.Expectation;
import beast.base.trace.LogAnalyser;
import beast.base.trace.TraceStatistics;
import beast.base.util.Randomizer;
import beast.pkgmgmt.BEASTClassLoader;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class TestFramework {
    protected static long SEED = 128;
    private String[] xmls;

    protected abstract List<Expectation> giveExpectations(int index_XML) throws Exception;

    public String logDir;
    public String testFile = "/test.";
    public boolean useSeed = true;
    public boolean checkESS = true;
    
    public TestFramework() {
    	logDir = System.getProperty("user.dir");
    }

    public static File readTestXML(String xmlName) throws URISyntaxException {
        String fullPath = "/beast.base/examples/beast2vs1/" + xmlName;
        var url = TestFramework.class.getResource(fullPath);
        if (url == null)
            throw new IllegalArgumentException("Test XML not found on classpath: " + fullPath);
        return Path.of(url.toURI()).toFile();
    }
    
    protected void setUp(String[] xmls) { // throws Exception {
        this.xmls = new String[xmls.length];
        for (int i = 0; i < xmls.length; i++) {
            this.xmls[i] = xmls[i];
        }

        Randomizer.setSeedReproducible(SEED);
        Logger.FILE_MODE = Logger.LogFileMode.overwrite;

        long beagleFlags = BeagleFlag.PROCESSOR_CPU.getMask() | BeagleFlag.VECTOR_SSE.getMask();
        System.setProperty("beagle.preferred.flags", Long.toString(beagleFlags));

        // add OneOnX for testing only
        BEASTClassLoader.addService(BEASTInterface.class.getName(), OneOnX.class.getName(), "BEAST.base");
    }
//    protected abstract void analyse() throws Exception;

    public void analyse(int index_XML) throws Exception {
//        for (int i = 0; i < xmls.length; i++) {
//            if (giveExpectations(i).size() > 0) {
//        Randomizer.setSeedReproducible(SEED);
//        Logger.FILE_MODE = Logger.LogFileMode.overwrite;
//
//        long beagleFlags = BeagleFlag.PROCESSOR_CPU.getMask() | BeagleFlag.VECTOR_SSE.getMask();
//        System.setProperty("beagle.preferred.flags", Long.toString(beagleFlags));

        File file = readTestXML(xmls[index_XML]);

        System.out.println("Processing " + file);
        XMLParser parser = new XMLParser();
        beast.base.inference.Runnable runable = parser.parseFile(file);
        runable.setStateFile("tmp.state", false);
//		   runable.setInputValue("preBurnin", 0);
//		   runable.setInputValue("chainLength", 1000);
        runable.run();

        final long seed = Randomizer.getSeed();
        String logFile = logDir + testFile + (useSeed ? seed : "") + ".log";
        System.out.println("\nAnalysing log " + logFile);
        LogAnalyser logAnalyser = new LogAnalyser(logFile, giveExpectations(index_XML)); // burnIn = 0.1 * maxState

        for (Expectation expectation : logAnalyser.m_pExpectations.get()) {
        	try {
            assertTrue(expectation.isPassed(),
            		xmls[index_XML] + ": Expected " + expectation.traceName.get() + " delta mean: "
	                    + expectation.expValue.get() + " - " + expectation.getTraceStatistics().getMean()
                    + " <= delta stdErr: " + stdErrMultiplier(expectation) + "*(" + expectation.getStdError() + " + "
                    + expectation.getTraceStatistics().getStdErrorOfMean() + ")");
        	} catch (NullPointerException e) {
        		System.err.println(expectation.traceName.get() + " might be missing from trace log");
        		throw e;
        	}
        	
            if (checkESS)
            	assertTrue(expectation.isValid(),
            			xmls[index_XML] + ":  has very low effective sample sizes (ESS) "
                    + expectation.getTraceStatistics().getESS());
        }

        System.out.println("\nSucceed " + file.getAbsolutePath());
        System.out.println("\n***************************************\n");
//            }
//        }
    }

    protected void addExpIntoList(List<Expectation> expList, String traceName, Double expValue, Double stdError) throws Exception {
        Expectation exp = new Expectation(traceName, expValue, stdError);
        expList.add(exp);
    }

    /**
     * Like {@link #addExpIntoList(List, String, Double, Double)}, but passes when
     * |mean - expValue| <= stdErrMultiplier * (stdError + stdErrorOfMean) instead of 2 * (...).
     * For tests that compare many correlated columns against a single-run reference, where
     * the default threshold (about 2.8 combined standard errors) fails on a sizeable share of seeds.
     */
    protected void addExpIntoList(List<Expectation> expList, String traceName, Double expValue, Double stdError,
                                  double stdErrMultiplier) throws Exception {
        expList.add(new ScaledExpectation(traceName, expValue, stdError, stdErrMultiplier));
    }

    private static double stdErrMultiplier(Expectation expectation) {
        return expectation instanceof ScaledExpectation s ? s.multiplier : 2.0;
    }

    private static class ScaledExpectation extends Expectation {
        final double multiplier;
        private boolean passed = true;
        private boolean valid = true;
        private TraceStatistics trace;

        ScaledExpectation(String traceName, Double expValue, Double stdError, double multiplier) throws Exception {
            super(traceName, expValue, stdError);
            this.multiplier = multiplier;
        }

        @Override
        public boolean assertExpectation(TraceStatistics trace, boolean displayStatistics) {
            passed = super.assertExpectation(trace, displayStatistics);
            valid = super.isValid();
            this.trace = trace;
            if (standErrorOfMean.get() == 0) {
                // a fixed value: keep the base check (mean must equal it) and skip the ESS check
                return passed;
            }
            double delta = Math.abs(trace.getMean() - expValue.get());
            passed = delta <= multiplier * (trace.getStdErrorOfMean() + standErrorOfMean.get());
            valid = trace.getESS() > 100;
            return passed;
        }

        @Override
        public boolean isPassed() { return passed; }

        @Override
        public boolean isValid() { return valid; }

        @Override
        public TraceStatistics getTraceStatistics() { return trace; }
    }

}