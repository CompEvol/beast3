package beast.base.spec.evolution.operator;

import beast.base.inference.State;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.distribution.Exponential;
import beast.base.spec.inference.operator.SampleOffValues;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.util.Randomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * SampleOffValues redraws the "off" values from {@code dist}. When {@code dist} is the
 * prior on the values, the move is a Gibbs move: its Hastings ratio must cancel the
 * change in the prior exactly, so that it is always accepted. Before this was fixed,
 * the operator returned only log dist(old), so a draw was accepted with probability
 * dist(new) (a density, so the result depended on the units of the values).
 *
 * <p>Uses the EBSP layout: one more value than indicators, and value 0 is always on.
 */
public class SampleOffValuesTest {

    private static final double[] START = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
    // indicator k switches value k+1; values 2, 4 and 5 are off
    private static final boolean[] INDICATORS = {true, false, true, false, false};

    private RealVectorParam<PositiveReal> values;
    private BoolVectorParam indicators;
    private Exponential prior;

    @BeforeEach
    void setUp() {
        Randomizer.setSeed(127);
        values = new RealVectorParam<>(START.clone(), PositiveReal.INSTANCE);
        values.setID("values");
        indicators = new BoolVectorParam(INDICATORS.clone());
        indicators.setID("indicators");
        State state = new State();
        state.initByName("stateNode", values, "stateNode", indicators);
        state.initialise();
        prior = new Exponential();
        prior.initByName("mean", new RealScalarParam<>(3.0, PositiveReal.INSTANCE));
    }

    private SampleOffValues operator(boolean all) {
        SampleOffValues op = new SampleOffValues();
        op.initByName("values", values, "indicators", indicators, "dist", prior, "all", all, "weight", 1.0);
        return op;
    }

    private double logPrior() {
        double logP = 0;
        for (int i = 0; i < values.size(); i++) {
            logP += prior.logDensity(values.get(i));
        }
        return logP;
    }

    /** log HR + change in log prior must be 0: a Gibbs move, always accepted. */
    private void assertGibbs(SampleOffValues op) {
        for (int k = 0; k < 1000; k++) {
            final double before = logPrior();
            final double logHR = op.proposal();
            final double after = logPrior();
            assertEquals(0.0, logHR + after - before, 1e-9, "log acceptance ratio at proposal " + k);
        }
    }

    @Test
    void testSingleValueMoveIsGibbs() {
        SampleOffValues op = operator(false);
        assertGibbs(op);
        assertOnValuesUnchanged();
        // every off value has been redrawn at least once
        for (int i : new int[]{2, 4, 5}) {
            assertNotEquals(START[i], values.get(i), "off value " + i + " was never redrawn");
        }
    }

    @Test
    void testAllValuesMoveIsGibbs() {
        SampleOffValues op = operator(true);
        assertGibbs(op);
        assertOnValuesUnchanged();
        // the last value is off and must be redrawn too (it used to be skipped)
        for (int i : new int[]{2, 4, 5}) {
            assertNotEquals(START[i], values.get(i), "off value " + i + " was never redrawn");
        }
    }

    private void assertOnValuesUnchanged() {
        for (int i : new int[]{0, 1, 3}) {
            assertEquals(START[i], values.get(i), 0.0, "on value " + i + " changed");
        }
    }
}
