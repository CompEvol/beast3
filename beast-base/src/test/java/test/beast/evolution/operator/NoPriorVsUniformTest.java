package test.beast.evolution.operator;

import beast.base.evolution.operator.kernel.BactrianScaleOperator;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.distribution.Uniform;
import beast.base.inference.operator.RealRandomWalkOperator;
import beast.base.inference.operator.kernel.BactrianRandomWalkOperator;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.util.ESS;
import org.apache.commons.math4.legacy.stat.StatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("slow")
public class NoPriorVsUniformTest extends BactrianRandomWalkOperatorTest {

    final long chainLength = 2000000;

    private final double lower = 1.0;
    private final double upper = 3.0;

    private final String start = "2.0";

    @BeforeEach
    void setUp() {
        // Fix seed: will hopefully ensure success of test unless something
        // goes terribly wrong.
//        Randomizer.setSeed(127);
    }

    @Override
    public void testNormalDistribution() {
    }

    @Override
    public void testLogNormalDistribution() {
    }

    @Test
    void testNoPriorBactrianRandomWalkOperator() {

        RealParameter parameter = new RealParameter(start);
        parameter.setLower(lower);
        parameter.setUpper(upper);

        BactrianRandomWalkOperator bactrianOperator = new BactrianRandomWalkOperator();
        // scaleFactor 0.75
        bactrianOperator.initByName("windowSize", 1.0, "weight", "1", "parameter", parameter);

        System.out.println("\nBactrianRandomWalkOperator");
        proposeNoPrior(parameter, bactrianOperator);
    }

    @Test
    void testNoPriorRandomWalkOperator() {

        RealParameter parameter = new RealParameter(start);
        parameter.setLower(lower);
        parameter.setUpper(upper);

        RealRandomWalkOperator scaleOperator = new RealRandomWalkOperator();
        // scaleFactor 0.75
        scaleOperator.initByName("windowSize", 1.0, "weight", "1", "parameter", parameter);

        System.out.println("\nRealRandomWalkOperator");
        proposeNoPrior(parameter, scaleOperator);
    }

    //TODO: Scale op is symmetric in log space, how to test in log space
//    @Test
//    void testNoPriorBactrianScale() {
//
//        RealParameter parameter = new RealParameter(start);
//        parameter.setLower(lower);
//        parameter.setUpper(upper);
//
//        BactrianScaleOperator bactrianOperator = new BactrianScaleOperator();
//        // scaleFactor 0.75
//        bactrianOperator.initByName("weight", "1", "parameter", parameter);
//
//        System.out.println("\nBactrianScaleOperator");
//        proposeNoPrior(parameter, bactrianOperator);
//    }
//
//    @Test
//    void testNoPriorScale() {
//
//        RealParameter parameter = new RealParameter(start);
//        parameter.setLower(lower);
//        parameter.setUpper(upper);
//
//        ScaleOperator scaleOperator = new ScaleOperator();
//        // scaleFactor 0.75
//        scaleOperator.initByName("weight", "1", "parameter", parameter);
//
//        System.out.println("\nScaleOperator");
//        proposeNoPrior(parameter, scaleOperator);
//    }

    private void proposeNoPrior(RealParameter param, Operator operator) {
        List<Double> values = new ArrayList<>();

        for (int i = 0; i < chainLength; i++) {
            double logHR = operator.proposal();
//            if (!Double.isFinite(logHR))
//                System.err.println("logHR = " + logHR + ", i = " + i + ", param = " + param);
            if (Double.isFinite(logHR))
                values.add(param.getValue());
        }

        System.out.println("\n----- Trace analysis -----------------------");
        double[] v = values.stream().mapToDouble(Double::doubleValue).toArray();
        double m = StatUtils.mean(v);
        double var = StatUtils.variance(v);
        double ess = ESS.calcESS(values);
        double acceptRatio = (double) v.length / (double) chainLength;
        System.out.println("Mean: " + m + " variance: " + var + " ESS: " + ess +
                ", n: " + v.length + ", accept ratio: " + acceptRatio);
        System.out.println("-------------------------------------------------");
        System.out.println();

        double min = Collections.min(values);
        double max = Collections.max(values);

        System.out.println("min = " + min + ", max = " + max);

        assertTrue(min >= lower, "Min = " + min);
        assertTrue(max <= upper, "Max = " + max);

        validateMean(var, ess, m);
    }

    private void validateMean(double var, double ess, double mean) {
        double standardErr = Math.sqrt(var) / Math.sqrt(ess);
        final double expectedM =  (upper + lower) / 2;
        final double expectedMeanLower = expectedM - 2 * standardErr;
        final double expectedMeanUpper = expectedM + 2 * standardErr;

        System.out.println("expectedM = " + expectedM + ", expectedMeanLower = " +
                expectedMeanLower + ", expectedMeanUpper = " + expectedMeanUpper);

        assertTrue(mean >= expectedM - 2 * standardErr && mean <= expectedM + 2 * standardErr,
                "expectedM = " + expectedM + ", standardErr = " + standardErr);
    }


    @Test
    void testUniformPrior() throws Exception {
        RealParameter parameter = new RealParameter(start);
        parameter.setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        Uniform uniform = new Uniform();
        uniform.initByName("lower", lower, "upper", upper);

        List<Double> values = doMCMC(parameter, uniform);

        double min = Collections.min(values);
        double max = Collections.max(values);

        System.out.println("min = " + min + ", max = " + max);

        assertTrue(min >= lower, "Min = " + min);
        assertTrue(max <= upper, "Max = " + max);

        double[] v = values.stream().mapToDouble(Double::doubleValue).toArray();
        double m = StatUtils.mean(v);
        double var = StatUtils.variance(v);
        double ess = ESS.calcESS(values);
        validateMean(var, ess, m);
    }

    // 1d param only
    private List<Double> doMCMC(RealParameter param, ParametricDistribution p) throws Exception {

        // Set up state:
        State state = new State();
        state.initByName("stateNode", param);

        // Set up operator:

        BactrianScaleOperator bactrianOperator = new BactrianScaleOperator();
        bactrianOperator.initByName("weight", "1", "parameter", param);

//		ScaleOperator bactrianOperator = new ScaleOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "scaleFactor", 0.75);

        // Set up logger:
        TraceReport traceReport = new TraceReport();
        traceReport.initByName(
                "logEvery", "10",
                "burnin", "2000",
                "log", param,
                "silent", false
        );

        Prior prior = new Prior();
        prior.initByName("x", param, "distr", p);
        // Set up MCMC:
        MCMC mcmc = new MCMC();
        mcmc.initByName(
                "chainLength", chainLength,
                "state", state,
                "distribution", prior,
                "operator", bactrianOperator,
                "logger", traceReport
        );

        // Run MCMC:
        mcmc.run();

        List<Double> values = traceReport.getAnalysis();
        return values;
    }
}
