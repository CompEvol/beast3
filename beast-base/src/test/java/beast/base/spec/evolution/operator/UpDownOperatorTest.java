package beast.base.spec.evolution.operator;


import beast.base.core.Loggable;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.LogNormal;
import beast.base.spec.inference.operator.ScaleOperator;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.util.AsRealScalar;
import beast.base.spec.inference.util.RPNcalculator;
import beast.base.spec.type.RealScalar;
import beast.base.util.Randomizer;
import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Tag("slow")
public class UpDownOperatorTest {//extends RealRandomWalkOperatorTest {

	@Test
	public void testLogNormalDistribution() throws Exception {

		// Set up operator:
        RealScalarParam<PositiveReal> param1 = new RealScalarParam<>(10.0, PositiveReal.INSTANCE);
		param1.setID("param1");
        RealScalarParam<PositiveReal> param2 = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
		param2.setID("param2");

        // TODO why not working?
        RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", param1, "parameter", param2,
                "expression", "param1 param2 -");

        //param1 param2 should be PositiveReal
        ScaleOperator scaleOperator1 = new ScaleOperator();
		scaleOperator1.initByName("weight", "0.5", "parameter", param1,
                "scaleFactor", 0.5, "optimise", false);
        ScaleOperator scaleOperator2 = new ScaleOperator();
		scaleOperator2.initByName("weight", "0.5", "parameter", param2,
                "scaleFactor", 0.5, "optimise", false);

        UpDownOperator bactrianOperator = new UpDownOperator();
        bactrianOperator.initByName("weight", "1", "up", param1, "down", param2,
                "scaleFactor", 0.025, "optimise", false);

        List<Operator> operators = new ArrayList<>();
		operators.add(bactrianOperator);
		operators.add(scaleOperator1);
		operators.add(scaleOperator2);

        AsRealScalar asRealScalar = new AsRealScalar();
        asRealScalar.initByName("arg", calculator, "domain", PositiveReal.INSTANCE);

		doMCMCrun(asRealScalar, param1, param2, operators);
	}

    // param must be RealScalar<PositiveReal> as required by LogNormal
	private void doMCMCrun(RealScalar<PositiveReal> param, Loggable param1, Loggable param2,
                           List<Operator> operators) throws IOException, SAXException, ParserConfigurationException {
		// Fix seed: will hopefully ensure success of test unless something
		// goes terribly wrong.
		Randomizer.setSeed(123);

        RealScalar<Real> meanInReal = new RealScalarParam<>(1.0, Real.INSTANCE);
        RealScalar<PositiveReal> sd = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
        LogNormal prior = new LogNormal(param, meanInReal, sd, true);

        // Set up state:
		State state = new State();
		Set<StateNode> stateNodes = new HashSet<>();
		for (Operator op : operators) {
			stateNodes.addAll(op.listStateNodes());
		}
		List<StateNode> list = new ArrayList<>();
		list.addAll(stateNodes);
		state.initByName("stateNode", list);


		// Set up logger:
		TraceReport traceReport = new TraceReport();
		traceReport.initByName(
				"logEvery", "10",
				"burnin", "2000",
				"log", param,
                "log", param1,
                "log", param2,
				"silent", false
				);

		// Set up MCMC:
		MCMC mcmc = new MCMC();
		mcmc.initByName(
				"chainLength", "5000000",
				"state", state,
				"distribution", prior,
				"operator", operators,
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
		assertEquals(1.0, m, 5e-3);
		assertEquals(Math.exp(-0.5), median, 5e-3);
		assertEquals(Math.exp(1)-1, s, 1e-1);
		assertEquals(0.0854, StatUtils.percentile(v, 2.5), 5e-3);
		assertEquals(0.117, StatUtils.percentile(v, 5), 5e-2);
		assertEquals(3.14, StatUtils.percentile(v, 95), 1e-1);
		assertEquals(4.31, StatUtils.percentile(v, 97.5), 1e-1);
	}
}
