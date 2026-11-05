package beast.base.spec.evolution.operator;


import beast.base.core.Loggable;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.Distribution;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.Exponential;
import beast.base.spec.inference.distribution.LogNormal;
import beast.base.spec.inference.operator.ScaleOperator;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.util.AsRealScalar;
import beast.base.spec.inference.util.RPNcalculator;
import beast.base.spec.type.RealScalar;
import beast.base.util.Randomizer;
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

		// param1 and param2 have no prior of their own, so with only the LogNormal prior on
		// x = param1 - param2 the posterior is improper: both drift upwards together (to about
		// 50-185 in 5M steps), mixing of x degrades, and its upper tail is under-sampled.
		// An Exponential(1) prior on param2 makes it proper; x and param2 are then independent,
		// so x is still exactly LogNormal.
		Exponential param2Prior = new Exponential();
		param2Prior.initByName("param", param2, "mean", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));

		doMCMCrun(asRealScalar, param1, param2, param2Prior, operators);
	}

    // param must be RealScalar<PositiveReal> as required by LogNormal
	private void doMCMCrun(RealScalar<PositiveReal> param, Loggable param1, Loggable param2,
                           Distribution param2Prior, List<Operator> operators) throws IOException, SAXException, ParserConfigurationException {
		// Fix seed: will hopefully ensure success of test unless something
		// goes terribly wrong.
		Randomizer.setSeedReproducible(123);

        RealScalar<Real> meanInReal = new RealScalarParam<>(1.0, Real.INSTANCE);
        RealScalar<PositiveReal> sd = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
        LogNormal xPrior = new LogNormal(param, meanInReal, sd, true);
        CompoundDistribution prior = new CompoundDistribution();
        prior.initByName("distribution", xPrior, "distribution", param2Prior);

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
		LogNormalSampleCheck.assertMean1Sigma1(v);
	}
}
