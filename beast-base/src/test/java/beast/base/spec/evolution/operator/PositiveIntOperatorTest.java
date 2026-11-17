package beast.base.spec.evolution.operator;

import beast.base.inference.State;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.inference.operator.IntRandomWalkOperator;
import beast.base.spec.inference.operator.uniform.IntUniformOperator;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.IntVectorParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integer operators on a PositiveInt parameter. PositiveInt is stored as the exclusive lower
 * bound 0, and getLower() used to return that 0, so an operator proposing down to getLower()
 * proposed 0 and IntScalarParam.set() threw "Value 0 is not valid for domain PositiveInt",
 * which ended the run (#287). getLower() is now the inclusive 1.
 */
public class PositiveIntOperatorTest {

    private static final int PROPOSALS = 10_000;

    @Test
    public void testIntRandomWalkNeverProposesZero() {
        IntScalarParam<PositiveInt> x = new IntScalarParam<>(1, PositiveInt.INSTANCE);
        State state = new State();
        state.initByName("stateNode", x);
        state.initialise();
        IntRandomWalkOperator operator = new IntRandomWalkOperator();
        operator.initByName("scalar", x, "windowSize", 2, "weight", 1.0);

        int atOne = 0;
        for (int i = 0; i < PROPOSALS; i++) {
            // every proposal is kept, so the walk keeps coming back to the boundary at 1
            assertDoesNotThrow(operator::proposal, "proposal " + i);
            assertTrue(x.get() >= 1, "proposed " + x.get());
            if (x.get() == 1) {
                atOne++;
            }
            if (x.get() > 5) {
                x.set(1);
            }
        }
        assertTrue(atOne > 0, "the walk never reached the boundary at 1");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testIntUniformNeverProposesZero() {
        IntVectorParam<PositiveInt> v = new IntVectorParam<>(new int[]{1, 1}, PositiveInt.INSTANCE);
        // the uniform operator draws between the bounds, so it needs a finite upper
        v.setUpper(5);
        State state = new State();
        state.initByName("stateNode", v);
        state.initialise();
        IntUniformOperator operator = new IntUniformOperator();
        operator.initByName("parameter", v, "howMany", 1, "weight", 1.0);

        int[] counts = new int[6];
        for (int i = 0; i < PROPOSALS; i++) {
            assertDoesNotThrow(operator::proposal, "proposal " + i);
            for (int value : v.getValues()) {
                assertTrue(value >= 1 && value <= 5, "proposed " + value);
                counts[value]++;
            }
        }
        assertEquals(0, counts[0]);
        for (int k = 1; k <= 5; k++) {
            assertTrue(counts[k] > 0, "value " + k + " never proposed");
        }
    }
}
