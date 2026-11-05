package test.beast.evolution.operator;

import beast.base.spec.evolution.operator.LogNormalSampleCheck;


import beast.base.core.Function;
import beast.base.evolution.operator.kernel.BactrianScaleOperator;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.Distribution;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.inference.distribution.Exponential;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.operator.kernel.BactrianUpDownOperator;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.util.RPNcalculator;
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

/**
 * @deprecated replaced by {@link beast.base.spec.evolution.operator.UpDownOperatorTest}
 */
@Tag("slow")
@Deprecated
public class BactrianUpDownOperatorTest extends BactrianRandomWalkOperatorTest {


	@Test
	@Override
	public void testNormalDistribution() throws Exception {
		// suppress test from super class
	}
	
	@Test
	public void testLogNormalDistribution() throws Exception {

		// Set up operator:
		RealParameter param1 = new RealParameter("10.0");
		param1.setID("param1");
		param1.setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		RealParameter param2 = new RealParameter("1.0");
		param2.setID("param2");
		param2.setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		BactrianUpDownOperator bactrianOperator = new BactrianUpDownOperator();
		bactrianOperator.initByName("weight", "1", "up", param1, "down", param2, "scaleFactor", 0.025, "optimise", false);
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", param1, "parameter", param2, "expression", "param1 param2 -");

		BactrianScaleOperator scaleOperator1 = new BactrianScaleOperator();
		scaleOperator1.initByName("weight", "0.5", "parameter", param1, "scaleFactor", 0.5, "optimise", false);
		BactrianScaleOperator scaleOperator2 = new BactrianScaleOperator();
		scaleOperator2.initByName("weight", "0.5", "parameter", param2, "scaleFactor", 0.5, "optimise", false);
		
		List<Operator> operators = new ArrayList<>();
		operators.add(bactrianOperator);
		operators.add(scaleOperator1);
		operators.add(scaleOperator2);
		
		// param1 and param2 have no prior of their own, so with only the LogNormal prior on
		// x = param1 - param2 the posterior is improper: both drift upwards together (to about
		// 50-185 in 5M steps), mixing of x degrades, and its upper tail is under-sampled.
		// An Exponential(1) prior on param2 makes it proper; x and param2 are then independent,
		// so x is still exactly LogNormal.
		Exponential exp = new Exponential();
		exp.initByName("mean", "1.0");
		Prior param2Prior = new Prior();
		param2Prior.initByName("x", param2, "distr", exp);

		doMCMCrun(calculator, param2Prior, operators);
	}
	


	

	
	void doMCMCrun(Function param, Distribution param2Prior, List<Operator> operators) throws IOException, SAXException, ParserConfigurationException {
		// Fix seed: will hopefully ensure success of test unless something
		// goes terribly wrong.
		Randomizer.setSeedReproducible(127);


		ParametricDistribution p = new LogNormalDistributionModel();
		p.initByName("M", "1.0", "S", "1.0", "meanInRealSpace", true);
		Prior xPrior = new Prior();
		xPrior.initByName("x", param, "distr", p);
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

		List<double[]> values = traceReport.getAnalysis2();
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i)[0];
		}
		LogNormalSampleCheck.assertMean1Sigma1(v);
	}
}
