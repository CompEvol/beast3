package beast.base.spec.evolution.operator;

import beast.base.inference.MCMC;
import beast.base.inference.State;
import beast.base.inference.operator.kernel.KernelDistribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.LogNormal;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.distribution.Uniform;
import beast.base.spec.inference.operator.RealRandomWalkOperator;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.util.ESS;
import beast.base.spec.type.RealScalar;
import beast.base.util.Randomizer;
import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class RealRandomWalkOperatorTest {

    @BeforeEach
    public void resetRng() {
        Randomizer.setSeed(127);
    }

	@Test
	public void testNormalDistribution() throws Exception {

		// Fix seed: will hopefully ensure success of test unless something
		// goes terribly wrong.
//		Randomizer.setSeed(127);

		// Assemble model:
        RealScalar<Real> param = new RealScalarParam<>(0.0, Real.INSTANCE);
        RealScalar<Real> mean = new RealScalarParam<>(1.0, Real.INSTANCE);
        RealScalar<PositiveReal> sigma = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
		Normal prior = new Normal(param, mean, sigma);

		// Set up state:
		State state = new State();
		state.initByName("stateNode", param);

// ESS 
// Mirror       196397.20218992446
// Bactrian		198525.37485263616
// Gaussian     177970.32054462744	
// non Gaussian 185569.35975056374		
		
		// Set up operator:
		RealRandomWalkOperator bactrianOperator = new RealRandomWalkOperator();
		KernelDistribution.Mirror kdist = new KernelDistribution.Mirror();
//		KernelDistribution.Bactrian kdist = new KernelDistribution.Bactrian();
		kdist.initByName("initial",500, "burnin", 500);
		bactrianOperator.initByName("weight", "1", "scalar", param,
                "kernelDistribution", kdist, "scaleFactor", 1.0, "optimise", true);

//		RealRandomWalkOperator bactrianOperator = new RealRandomWalkOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "windowSize", 1.0, "useGaussian", false);

		// Set up logger:
		TraceReport traceReport = new TraceReport();
		traceReport.initByName(
				"logEvery", "10",
				"burnin", "2000",
				"log", param,
				"silent", false
				);

		// Set up MCMC:
		MCMC mcmc = new MCMC();
		mcmc.initByName(
				"chainLength", "2000000",
				"state", state,
				"distribution", prior,
				"operator", bactrianOperator,
				"logger", traceReport
				);

		// Run MCMC:
		mcmc.run();

		List<Double> values = traceReport.getAnalysis();
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i);
		}
		double m = StatUtils.mean(v);
		double s = StatUtils.variance(v);
		assertEquals(1.0, m, 5e-3);
		assertEquals(1.0, s, 5e-3);

	}
	
	@Test
	public void testLogNormalDistribution() throws Exception {

		// Fix seed: will hopefully ensure success of test unless something
		// goes terribly wrong.
//		Randomizer.setSeed(131);

		// Assemble model:
        RealScalar<PositiveReal> param = new RealScalarParam<>(10.0, PositiveReal.INSTANCE);
        RealScalar<Real> meanInReal = new RealScalarParam<>(1.0, Real.INSTANCE);
        RealScalar<PositiveReal> sd = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
        LogNormal prior = new LogNormal(param, meanInReal, sd, true);

		// Set up state:
		State state = new State();
		state.initByName("stateNode", param);

// ESS 
// Mirror       101392.25428944119 
// Bactrian		 40484.42378045924 Mean: 0.9936652025880692 variance: 1.6192894315845616
// Gaussian      58971.221113274	
// Uniform       79520.29358602439		
		
		// Set up operator:
        RealRandomWalkOperator bactrianOperator = new RealRandomWalkOperator();
// 		KernelDistribution.MirrorDistribution kdist = new KernelDistribution.MirrorDistribution();
		KernelDistribution.Bactrian kdist = new KernelDistribution.Bactrian();
		kdist.initAndValidate();
		bactrianOperator.initByName("weight", "1", "scalar", param,
                "kernelDistribution", kdist, "scaleFactor", 1.0, "optimise", true);

//		RealRandomWalkOperator bactrianOperator = new RealRandomWalkOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "windowSize", 1.0, "useGaussian", true);

		// Set up logger:
		TraceReport traceReport = new TraceReport();
		traceReport.initByName(
				"logEvery", "10",
				"burnin", "2000",
				"log", param,
				"silent", false
				);

		// Set up MCMC:
		MCMC mcmc = new MCMC();
		mcmc.initByName(
				"chainLength", "2000000",
				"state", state,
				"distribution", prior,
				"operator", bactrianOperator,
				"logger", traceReport
				);

		// Run MCMC:
		mcmc.run();

		List<Double> values = traceReport.getAnalysis();
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i);
		}
		double m = StatUtils.mean(v);
		double median = StatUtils.percentile(v, 50);
		double s = StatUtils.variance(v, 50);
		assertEquals(1.0, m, 9e-3);
		assertEquals(Math.exp(-0.5), median, 9e-3);
		assertEquals(Math.exp(1)-1, s, 1e-1);
		assertEquals(0.0854, StatUtils.percentile(v, 2.5), 5e-3);
		assertEquals(0.117, StatUtils.percentile(v, 5), 5e-3);
		assertEquals(3.14, StatUtils.percentile(v, 95), 1e-1);
		assertEquals(4.31, StatUtils.percentile(v, 97.5), 1e-1);

	}

    @Test
    void testUniformPrior() throws Exception {
        final double lower = 1.0;
        final double upper = 3.0;
        RealScalarParam<Real> parameter = new RealScalarParam<>(1.0, Real.INSTANCE);
        // set bounds
        Uniform uniform = new Uniform(parameter,
                new RealScalarParam<>(lower, Real.INSTANCE),
                new RealScalarParam<>(upper, Real.INSTANCE));
        // if ID is null the bounds setting will not work
        parameter.setID("p1");

        // check bounds
        assertEquals(lower, parameter.getLower(), 1e-10);
        assertEquals(upper, parameter.getUpper(), 1e-10);

        // Set up state:
        State state = new State();
        state.initByName("stateNode", parameter);

        // Set up operator:
        RealRandomWalkOperator bactrianOperator = new RealRandomWalkOperator();
// 		KernelDistribution.MirrorDistribution kdist = new KernelDistribution.MirrorDistribution();
        KernelDistribution.Bactrian kdist = new KernelDistribution.Bactrian();
        kdist.initAndValidate();
        bactrianOperator.initByName("weight", "1", "scalar", parameter,
                "kernelDistribution", kdist, "windowSize", 0.1, "optimise", true);

        // Set up logger:
        TraceReport traceReport = new TraceReport();
        traceReport.initByName(
                "logEvery", "10",
                "burnin", "2000",
                "log", parameter,
                "silent", false
        );

        // Set up MCMC:
        MCMC mcmc = new MCMC();
        mcmc.initByName(
                "chainLength", "2000000",
                "state", state,
                "distribution", uniform,
                "operator", bactrianOperator,
                "logger", traceReport
        );

        // Run MCMC:
        mcmc.run();

        List<Double> values = traceReport.getAnalysis();

        double min = Collections.min(values);
        double max = Collections.max(values);

        System.out.println("min = " + min + ", max = " + max);

        assertTrue(min >= lower, "Min = " + min);
        assertTrue(max <= upper, "Max = " + max);

        double[] v = values.stream().mapToDouble(Double::doubleValue).toArray();
        double mean = StatUtils.mean(v);
        double var = StatUtils.variance(v);
        double ess = ESS.calcESS(values);
        System.out.println("mean = " + mean + ", var = " +
                var + ", ESS = " + ess + ", length = " + v.length);

        double standardErr = Math.sqrt(var) / Math.sqrt(ess);
        final double expectedM =  (upper + lower) / 2;
        final double expectedMeanLower = expectedM - 2 * standardErr;
        final double expectedMeanUpper = expectedM + 2 * standardErr;

        System.out.println("expectedM = " + expectedM + ", expectedMeanLower = " +
                expectedMeanLower + ", expectedMeanUpper = " + expectedMeanUpper);

        assertEquals(expectedM, mean, 9e-3);
        assertTrue(mean >= expectedM - 2 * standardErr && mean <= expectedM + 2 * standardErr,
                "expectedM = " + expectedM + ", standardErr = " + standardErr);
    }
}
