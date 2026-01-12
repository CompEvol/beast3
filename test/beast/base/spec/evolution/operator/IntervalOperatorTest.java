package beast.base.spec.evolution.operator;

import beast.base.inference.Distribution;
import beast.base.inference.MCMC;
import beast.base.inference.State;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.distribution.TruncatedReal;
import beast.base.spec.inference.distribution.Uniform;
import beast.base.spec.inference.operator.uniform.IntervalOperator;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.RealScalar;
import beast.base.util.Randomizer;
import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class IntervalOperatorTest {

    @BeforeEach
    void setUp() {
        // Fix seed: will hopefully ensure success of test unless something
        // goes terribly wrong.
        Randomizer.setSeed(127);
    }


    @Test
    public void testRealScalarBound() {
        try {
            final double lower = 0.0;
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

            State state = new State();
            state.initByName("stateNode", parameter);
            state.initialise();

            IntervalOperator intervalOperator = new IntervalOperator();
            intervalOperator.initByName("parameter", parameter, "weight", 1.0);

            System.out.println("IntervalOperator on RealScalarParam : " +
                    "value excludes bounds, where lower = 0.0 and upper = 3.0 ");
            for (int i = 0; i < 400; i++) {
                intervalOperator.proposal();
                double value = parameter.get();
                assertTrue(value > lower && value < upper,
                        "IntervalOperator on RealScalarParam, value in (" +
                                lower + ", " + upper + ") + " + value);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * IntervalOperator's move excludes bounds
     */
    @Test
    public void testRealVectorBound() {
        try {

            final double lower = 0.0;
            final double upper = 3.0;

            RealVectorParam<Real> parameter = new RealVectorParam<>(new double[]{1.0, 0.1, 2.0}, PositiveReal.INSTANCE);

            //TODO
            parameter.setLower(lower);
	        parameter.setUpper(upper);

//            Uniform uniform = new Uniform(null,
//                    new RealScalarParam<>(0, Real.INSTANCE),
//                    new RealScalarParam<>(3, Real.INSTANCE));
//
//            IID iid = new IID(parameter, uniform);
//
//            assertEquals(0.0, (Double) iid.getLower(), 1e-10);
//            assertEquals(3.0, (Double) iid.getUpper(), 1e-10);

            State state = new State();
            state.initByName("stateNode", parameter);
            state.initialise();

            IntervalOperator intervalOperator = new IntervalOperator();
            intervalOperator.initByName("parameter", parameter, "weight", 1.0);

            System.out.println("IntervalOperator on RealVectorParam : " +
                    "value excludes bounds, where lower = 0.0 and upper = 3.0 ");
            for (int i = 0; i < 400; i++) {
                intervalOperator.proposal();
                for (int j = 0; j < parameter.size(); j++) {
                    double value = parameter.get(j);
                    assertTrue(value > lower && value < upper,
                            "IntervalOperator on RealVectorParam, value in (" + lower + ", " +
                                    upper + ") + " + value + " at dim " + j);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
	public void testTruncatedNormalDistribution() throws Exception {
		// Set up prior:
        RealScalar<Real> mean = new RealScalarParam<>(1.5, Real.INSTANCE);
        RealScalar<PositiveReal> sigma = new RealScalarParam<>(0.1, PositiveReal.INSTANCE);
        Normal normal = new Normal(null, mean, sigma);

        RealScalarParam<Real> param = new RealScalarParam<>(2.0, Real.INSTANCE);
        // replace param.setBounds(1.0,3.0); to this
        TruncatedReal prior = new TruncatedReal();
        prior.initByName(
                "param", param,
                "distribution", normal,
                "lower", new RealScalarParam<>(1.0, Real.INSTANCE),
                "upper", new RealScalarParam<>(3.0, Real.INSTANCE)
        );

        // Test  bounds
        assertEquals(1.0, prior.getLower(), 1e-10);
        assertEquals(3.0, prior.getUpper(), 1e-10);
        assertEquals(1.0, param.getLower(), 1e-10);
        assertEquals(3.0, param.getUpper(), 1e-10);

        List<double[]> values = doMCMC(param, prior);
		
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i)[0];
		}
		double m = StatUtils.mean(v);
		double s = StatUtils.variance(v);
		assertEquals(1.5, m, 5e-3);
		assertEquals(0.01, s, 5e-3);
		
		/*
		effective sample size (ESS)	
		Bactrian 151680.3	
		Uniform   62021.1
		*/
	}

	@Test
	public void testUniformDistribution() throws Exception {
		// Set up prior:
        RealScalarParam<Real> param = new RealScalarParam<>(2.0, Real.INSTANCE);
        RealScalar<Real> lower = new RealScalarParam<>(1.0, Real.INSTANCE);
        RealScalar<Real> upper = new RealScalarParam<>(3.0, Real.INSTANCE);
        Uniform prior = new Uniform(param, lower, upper);

		List<double[]> values = doMCMC(param, prior);
		
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i)[0];
		}
		double m = StatUtils.mean(v);
		double s = StatUtils.variance(v);
		// mean = (upper - lower)/ 2
		assertEquals(2.0, m, 5e-3);
		// variance = (upper - lower)^2 / 12
		assertEquals((3-1)*(3-1)/12.0, s, 5e-3);

	}
	
	@Test
	public void testUniformDistributionZeroLowerBound() throws Exception {
		// Set up prior:
        RealScalarParam<Real> param = new RealScalarParam<>(1.0, Real.INSTANCE);
        RealScalar<Real> lower = new RealScalarParam<>(0.0, Real.INSTANCE);
        RealScalar<Real> upper = new RealScalarParam<>(2.0, Real.INSTANCE);
        Uniform prior = new Uniform(param, lower, upper);


		List<double[]> values = doMCMC(param, prior);
		
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i)[0];
		}
		double m = StatUtils.mean(v);
		double s = StatUtils.variance(v);
		assertEquals(1.0, m, 5e-3);
		assertEquals(1.0/3.0, s, 5e-3);

	}
	
	@Test
	public void testUniformDistributionZeroUpperBound() throws Exception {
		// Set up prior:
        RealScalarParam<Real> param = new RealScalarParam<>(-1.0, Real.INSTANCE);
        RealScalar<Real> lower = new RealScalarParam<>(-2.0, Real.INSTANCE);
        RealScalar<Real> upper = new RealScalarParam<>(0.0, Real.INSTANCE);
        Uniform prior = new Uniform(param, lower, upper);

		List<double[]> values = doMCMC(param, prior);
		
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i)[0];
		}
		double m = StatUtils.mean(v);
		double s = StatUtils.variance(v);
		assertEquals(-1.0, m, 5e-3);
		assertEquals(1.0/3.0, s, 5e-3);

		/**
		effective sample size (ESS)	
		Bactrian: 177235.9
	  	Uniform:  179821.0
		*/
	}
	
	private List<double[]> doMCMC(RealScalarParam<Real> param, Distribution prior) throws Exception {

		// Set up state:
		State state = new State();
		state.initByName("stateNode", param);

		// Set up operator:
		IntervalOperator bactrianOperator = new IntervalOperator();
		bactrianOperator.initByName("weight", "1", "parameter", param);

//		UniformOperator bactrianOperator = new UniformOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param);

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

		List<double[]> values = traceReport.getAnalysis2();
		return values;
	}

}
