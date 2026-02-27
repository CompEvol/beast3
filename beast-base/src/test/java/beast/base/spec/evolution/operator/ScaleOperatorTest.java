package beast.base.spec.evolution.operator;

import beast.base.inference.*;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.IID;
import beast.base.spec.inference.distribution.LogNormal;
import beast.base.spec.inference.operator.ScaleOperator;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.RealScalar;
import beast.base.util.Randomizer;
import org.apache.commons.math4.legacy.stat.StatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Tag("slow")
public class ScaleOperatorTest {//extends RealRandomWalkOperatorTest {

    @BeforeEach
    void setUp() {
        // Fix seed: will hopefully ensure success of test unless something
        // goes terribly wrong.
        Randomizer.setSeed(127);
    }

//    @Test
//	@Override
//	public void testNormalDistribution() throws Exception {
//		// suppress test from super class
//	}
	
	@Test
	public void testLogNormalDistribution() throws Exception {

		// Set up operator:
        RealScalarParam<PositiveReal> param = new RealScalarParam<>(10.0, PositiveReal.INSTANCE);
		ScaleOperator scalarOperator = new ScaleOperator();
		scalarOperator.initByName("weight", "1", "parameter", param);

//		ScaleOperator scalarOperator = new ScaleOperator();
//		scalarOperator.initByName("weight", "1", "parameter", param, "scaleFactor", 0.75);
		
		doMCMCrun(param, scalarOperator);
	}

	
	@Test
	public void testTwoDimensionalDistribution() throws Exception {
		// Set up operator:
        RealVectorParam<PositiveReal> param = new RealVectorParam<>(new double[]{10.0, 10.0}, PositiveReal.INSTANCE);
        ScaleOperator scalarOperator = new ScaleOperator();
        scalarOperator.initByName("weight", "1", "parameter", param);

//		ScaleOperator bactrianOperator = new ScaleOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "scaleFactor", 0.75);

		doMCMCrun(param, scalarOperator, null);
		
	 /* Results:
		Effective sample size (ESS):	
		BactrianScaleOperator 109202.7	114451.9	
		ScaleOperator          24587.3	 22223.3
	 */
	}
	
	
	@Test
	public void testScaleAllIndependentlylDistribution() throws Exception {
		// Set up operator:
        RealVectorParam<PositiveReal> param = new RealVectorParam<>(new double[]{10.0, 10.0, 10.0}, PositiveReal.INSTANCE);

        // Set up operator:
        ScaleOperator scalarOperator = new ScaleOperator();
        scalarOperator.initByName("weight", "1", "parameter", param, "scaleFactor", 0.75, "scaleAllIndependently", true);

//		ScaleOperator bactrianOperator = new ScaleOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "scaleFactor", 0.75, "scaleAllIndependently", true);

		doMCMCrun(param, scalarOperator, null);

		
	 /* Results:
		Effective sample size (ESS):	
		BactrianScaleOperator  143614.1	144363.9	142207.5	 	
		ScaleOperator           30736.7	 29254.7	 30813.9
	 */
	}
	
	@Test
	public void testScaleAllDistribution() throws Exception {

		// Set up operator:
        RealVectorParam<PositiveReal> param = new RealVectorParam<>(new double[]{10.0, 10.0, 10.0}, PositiveReal.INSTANCE);

        // Set up operator:
        ScaleOperator scalarOperator = new ScaleOperator();
        scalarOperator.initByName("weight", "1", "parameter", param, "scaleAll", true);

//		ScaleOperator bactrianOperator = new ScaleOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "scaleFactor", 0.75, "scaleAll", true);
		
		// requires second scale operator to assure dimensions are independent (which the first operator assumes)
        ScaleOperator scalarOperator2 = new ScaleOperator();
        scalarOperator2.initByName("weight", "1", "parameter", param);

		doMCMCrun(param, scalarOperator, scalarOperator2);
		
	 /* Results:
		Effective sample size (ESS):	
		BactrianScaleOperator  67413.3	65668.3	67986.6	 	
		ScaleOperator          58268	56123.4	51362.7
	 */
	}

	final static double EPSILON = 1e-10;

//	@Test
//	public void testTreeScaling() {
//        String newick = "((0:1.0,1:1.0)4:1.0,(2:1.0,3:1.0)5:0.5)6:0.0;";
//
//        TreeParser tree = new TreeParser(newick, false, false, false, 0);
//
//        Node [] node = tree.getNodesAsArray();
//
//        BactrianScaleOperator operator = new BactrianScaleOperator();
//        operator.initByName("tree", tree, "weight", 1.0);
//        operator.proposal();
//
//        // leaf node
//        node = tree.getNodesAsArray();
//        assertEquals(0.0, node[0].getHeight(), EPSILON);
//        assertEquals(0.0, node[1].getHeight(), EPSILON);
//        // leaf node, not scaled
//        assertEquals(0.5, node[2].getHeight(), EPSILON);
//        assertEquals(0.5, node[3].getHeight(), EPSILON);
//
//        // internal nodes, all scaled
//        // first determine scale factor
//        double scale = node[4].getHeight() / 1.0;
//        assertEquals(1.0 * scale, node[4].getHeight(), EPSILON);
//        assertEquals(1.5 * scale, node[5].getHeight(), EPSILON);
//        assertEquals(2.0 * scale, node[6].getHeight(), EPSILON);
//	}

	
	void doMCMCrun(RealVectorParam<PositiveReal> vectorParam, Operator bactrianOperator, Operator optionOperator) throws IOException, SAXException, ParserConfigurationException {
        IID prior = getIIDLogNormal(vectorParam);

        doMCMCrun(vectorParam, prior, bactrianOperator, optionOperator);
	}

    void doMCMCrun(RealScalarParam<PositiveReal> scalarParam, Operator bactrianOperator) throws IOException, SAXException, ParserConfigurationException {
        LogNormal prior = getLogNormal(scalarParam);

        doMCMCrun(scalarParam, prior, bactrianOperator, null);
    }


    private void doMCMCrun(StateNode param, Distribution prior, Operator bactrianOperator, Operator optionOperator) throws IOException, SAXException, ParserConfigurationException {

        // Fix seed: will hopefully ensure success of test unless something
        // goes terribly wrong.
//        Randomizer.setSeed(127);

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
		if (optionOperator == null) {
			mcmc.initByName(
					"chainLength", "1000000",
					"state", state,
					"distribution", prior,
					"operator", bactrianOperator,
					"logger", traceReport
					);
		} else {
			mcmc.initByName(
					"chainLength", "1000000",
					"state", state,
					"distribution", prior,
					"operator", bactrianOperator,
					"operator", optionOperator,
					"logger", traceReport
					);			
		}

		// Run MCMC:
		mcmc.run();

		List<double[]> values = traceReport.getAnalysis2();
		for (int dim = 0; dim < values.getFirst().length; dim++) {
			double[] v = new double[values.size()];
			for (int i = 0; i < v.length; i++) {
				v[i] = values.get(i)[dim];
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
	}

    private LogNormal getLogNormal(RealScalarParam<PositiveReal> scalarParam) {
        RealScalar<Real> meanInReal = new RealScalarParam<>(1.0, Real.INSTANCE);
        RealScalar<PositiveReal> sd = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
        return new LogNormal(scalarParam, meanInReal, sd, true);
    }

    private IID<RealVectorParam<PositiveReal>, RealScalar<PositiveReal>, Double>
                getIIDLogNormal(RealVectorParam<PositiveReal> vectorParam) {
        // only distr here
        LogNormal logNormal = getLogNormal(null);

        return new IID<>(vectorParam, logNormal);
    }
}
