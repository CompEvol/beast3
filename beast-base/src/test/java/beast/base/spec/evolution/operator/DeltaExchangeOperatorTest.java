/**
 * 
 */
package beast.base.spec.evolution.operator;

import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.distribution.Dirichlet;
import beast.base.spec.inference.operator.DeltaExchangeOperator;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.SimplexParam;
import beast.base.spec.type.Simplex;
import beast.base.util.Randomizer;
import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import test.beast.evolution.operator.TestOperator;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author gereon
 *
 */
@Tag("slow")
public class DeltaExchangeOperatorTest extends TestOperator {

	@Test
	public void testKeepsSum() {
		DeltaExchangeOperator operator = new DeltaExchangeOperator();
		RealVectorParam<PositiveReal> parameter = new RealVectorParam<>(new double[] {1., 1., 1., 1.}, PositiveReal.INSTANCE);
		register(operator,
				"rvparameter", parameter);
		for (int i=0; i<100; ++i) {
			operator.proposal();
		}
		double i = 0;
		for (Double p : parameter.getElements()) {
			i += p;
		}
		assertEquals(4, i, 0.00001, "The DeltaExchangeOperator should not change the sum of a parameter");
	}
	
	@Test
	public void testKeepsWeightedSum() {
        RealVectorParam<PositiveReal> parameter = new RealVectorParam<>(new double[] {1., 1., 1., 1.}, PositiveReal.INSTANCE);
        register(new DeltaExchangeOperator(),
				"weightvector", new IntVectorParam<>(new int[] {0, 1, 2, 1}, NonNegativeInt.INSTANCE),
				"rvparameter", parameter);
		Double[] p = parameter.getElements().toArray(new Double[0]);
		assertEquals(4, 0*p[1]+1*p[1]+2*p[2]+1*p[3], 0.00001,
				"The DeltaExchangeOperator should not change the sum of a parameter");
	}
	
	@Test
	public void testCanOperate() {
		// Test whether a validly initialised operator may make proposals
		State state = new State();
        RealVectorParam<PositiveReal> parameter = new RealVectorParam<>(new double[] {1., 1., 1., 1.}, PositiveReal.INSTANCE);
        state.initByName("stateNode", parameter);
		state.initialise();
		DeltaExchangeOperator d = new DeltaExchangeOperator();
		// An invalid operator should either fail in initByName or make valid
		// proposals
		try {
			d.initByName("rvparameter", parameter, "weight", 1.0);
		} catch (RuntimeException e) {
			return;
		}
		d.proposal();
	}

    @Test
    public void testLogNormalDistribution() throws Exception {

        // Set up operator:
        SimplexParam param1 = new SimplexParam(new double[]{0.25, 0.25, 0.25, 0.25});
        param1.setID("param1");
        // param1.setBounds(0.0,  1.0);
        Operator bactrianOperator = new DeltaExchangeOperator();
        // gives ESS: 18886.199729294272
        //Operator bactrianOperator = new DeltaExchangeOperator();
        // gives ESS:  1008.3640684048069
        bactrianOperator.initByName("weight", "1", "parameter", param1, "delta", 0.24, "autoOptimize", true);

        doMCMCrun(param1, bactrianOperator);
    }

    private void doMCMCrun(Simplex param, Operator operators) throws IOException, SAXException, ParserConfigurationException {
        // Fix seed: will hopefully ensure success of test unless something
        // goes terribly wrong.
        Randomizer.setSeed(127);

        Dirichlet prior = new Dirichlet();
        RealVectorParam<PositiveReal> alpha = new RealVectorParam<>(new double[]{4.0, 1.0, 1.0, 1.0}, PositiveReal.INSTANCE);
        prior.initByName("param", param, "alpha", alpha);

        // Set up state:
        State state = new State();
        state.initByName("stateNode", param);

        // Set up logger:
        TraceReport traceReport = new TraceReport();
        traceReport.initByName(
                "logEvery", "10",
                "burnin", "2000",
                "log", param,
                "silent", true
        );

        // Set up MCMC:
        MCMC mcmc = new MCMC();
        mcmc.initByName(
                "chainLength", "1000000",
                "state", state,
                "distribution", prior,
                "operator", operators,
                "logger", traceReport
        );

        // Run MCMC:
        mcmc.run();

        List<double[]> values = traceReport.getAnalysis2();
        double[] v = new double[values.size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = values.get(i)[0];
        }
        double m = StatUtils.mean(v);
        // double median = StatUtils.percentile(v, 50);
        double s = StatUtils.variance(v, 50);
        assertEquals(4.0/7.0, m, 5e-3);

        double var = 4.0/7.0 * (1.0 - 4.0/7.0) / 8.0;
        assertEquals(var, s, 5e-3);
    }

}
