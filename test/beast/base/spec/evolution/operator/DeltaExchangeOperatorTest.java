/**
 * 
 */
package beast.base.spec.evolution.operator;

import beast.base.inference.State;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.operator.DeltaExchangeOperator;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import org.junit.jupiter.api.Test;
import test.beast.evolution.operator.TestOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author gereon
 *
 */
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
				"weightvector", new IntVectorParam<>(new int[] {0, 1, 2, 1}, PositiveInt.INSTANCE),
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

}
