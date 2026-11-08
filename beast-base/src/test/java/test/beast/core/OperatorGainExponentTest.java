package test.beast.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import beast.base.evolution.operator.ScaleOperator;
import beast.base.evolution.operator.SubtreeSlide;
import beast.base.evolution.operator.kernel.BactrianSubtreeSlide;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.Operator;
import beast.base.inference.OperatorSchedule;
import beast.base.inference.parameter.RealParameter;

/**
 * The auto-tuning gain 1/n^e of OperatorSchedule.calcDelta: the schedule's gainExponent,
 * an operator's own gainExponent (input or default), and the precedence between them:
 * operator input, then the schedule's gainExponent or transform, then the operator's default,
 * then the classic 1/n.
 */
public class OperatorGainExponentTest {

    /** accepted proposals before calcDelta, so the count used by calcDelta is ACCEPTED + 1 */
    static final int ACCEPTED = 9;
    static final double COUNT = ACCEPTED + 1;

    static OperatorSchedule schedule(Object... args) {
        OperatorSchedule schedule = new OperatorSchedule();
        Object[] all = new Object[args.length + 2];
        all[0] = "autoOptimizeDelay";
        all[1] = 0;
        System.arraycopy(args, 0, all, 2, args.length);
        schedule.initByName(all);
        return schedule;
    }

    static ScaleOperator scaleOperator(Object... extra) {
        ScaleOperator op = new ScaleOperator();
        op.setID("scale");
        Object[] all = new Object[extra.length + 4];
        all[0] = "parameter";
        all[1] = new RealParameter(new Double[] {1.0});
        all[2] = "weight";
        all[3] = 1.0;
        System.arraycopy(extra, 0, all, 4, extra.length);
        op.initByName(all);
        return op;
    }

    static BactrianSubtreeSlide slide(Object... extra) {
        BactrianSubtreeSlide op = new BactrianSubtreeSlide();
        op.setID("slide");
        return initTreeOperator(op, extra);
    }

    static SubtreeSlide classicSlide(Object... extra) {
        SubtreeSlide op = new SubtreeSlide();
        op.setID("classicSlide");
        return initTreeOperator(op, extra);
    }

    static <T extends Operator> T initTreeOperator(T op, Object... extra) {
        Tree tree = new TreeParser("((A:1.0,B:1.0):1.0,(C:1.5,D:1.5):0.5);", false);
        Object[] all = new Object[extra.length + 4];
        all[0] = "tree";
        all[1] = tree;
        all[2] = "weight";
        all[3] = 1.0;
        System.arraycopy(extra, 0, all, 4, extra.length);
        op.initByName(all);
        return op;
    }

    /** delta after ACCEPTED accepted proposals, for a proposal accepted with probability 1 */
    static double delta(OperatorSchedule schedule, Operator op) {
        schedule.addOperator(op);
        for (int i = 0; i < ACCEPTED; i++) {
            op.accept();
        }
        return schedule.calcDelta(op, 0.0);
    }

    static double expected(Operator op, double exponent) {
        return (1.0 - op.getTargetAcceptanceProbability()) / Math.pow(COUNT, exponent);
    }

    @Test
    public void defaultGainIsOneOverN() {
        ScaleOperator op = scaleOperator();
        assertEquals(Double.NaN, op.getGainExponent());
        assertEquals(expected(op, 1.0), delta(schedule(), op), 1e-15);
    }

    @Test
    public void scheduleGainExponent() {
        ScaleOperator op = scaleOperator();
        assertEquals(expected(op, 0.6), delta(schedule("gainExponent", 0.6), op), 1e-15);
    }

    @Test
    public void scheduleGainExponentOneMatchesDefault() {
        ScaleOperator a = scaleOperator();
        ScaleOperator b = scaleOperator();
        assertEquals(delta(schedule(), a), delta(schedule("gainExponent", 1.0), b), 1e-15);
    }

    @Test
    public void scheduleGainExponentHalfMatchesSqrtTransform() {
        ScaleOperator a = scaleOperator();
        ScaleOperator b = scaleOperator();
        assertEquals(delta(schedule("transform", "sqrt"), a), delta(schedule("gainExponent", 0.5), b), 1e-15);
    }

    @Test
    public void operatorGainExponentOverridesSchedule() {
        ScaleOperator op = scaleOperator("gainExponent", 1.0);
        assertEquals(expected(op, 1.0), delta(schedule("transform", "sqrt"), op), 1e-15);
        ScaleOperator op2 = scaleOperator("gainExponent", 0.7);
        assertEquals(expected(op2, 0.7), delta(schedule("gainExponent", 0.5), op2), 1e-15);
    }

    @Test
    public void subtreeSlideDefaultsToSixTenths() {
        BactrianSubtreeSlide op = slide();
        assertEquals(0.6, op.getGainExponent());
        assertEquals(expected(op, 0.6), delta(schedule(), op), 1e-15);
    }

    @Test
    public void classicSubtreeSlideDefaultsToSixTenths() {
        SubtreeSlide op = classicSlide();
        assertEquals(0.6, op.getGainExponent());
        assertEquals(expected(op, 0.6), delta(schedule(), op), 1e-15);
    }

    @Test
    public void scheduleSettingsOverrideSubtreeSlideDefault() {
        // an explicit schedule gainExponent or transform wins over the slide's built-in 0.6
        BactrianSubtreeSlide a = slide();
        assertEquals(expected(a, 1.0), delta(schedule("gainExponent", 1.0), a), 1e-15);
        BactrianSubtreeSlide b = slide();
        assertEquals(expected(b, 0.5), delta(schedule("transform", "sqrt"), b), 1e-15);
        SubtreeSlide c = classicSlide();
        assertEquals(expected(c, 1.0), delta(schedule("gainExponent", 1.0), c), 1e-15);
    }

    @Test
    public void subtreeSlideInputOverridesSchedule() {
        BactrianSubtreeSlide op = slide("gainExponent", 1.0);
        assertEquals(expected(op, 1.0), delta(schedule(), op), 1e-15);
        BactrianSubtreeSlide op2 = slide("gainExponent", 0.8);
        assertEquals(expected(op2, 0.8), delta(schedule("gainExponent", 0.6), op2), 1e-15);
    }

    @Test
    public void invalidExponentsAreRejected() {
        // initByName wraps exceptions from initAndValidate in a RuntimeException
        assertThrows(RuntimeException.class, () -> schedule("gainExponent", 1.5));
        assertThrows(RuntimeException.class, () -> schedule("gainExponent", 0.0));
        assertThrows(RuntimeException.class, () -> schedule("gainExponent", 0.6, "transform", "sqrt"));
        // an operator's bad exponent fails when it is added to a schedule, not mid-run
        for (double bad : new double[] {-1.0, 0.0, 1.0001}) {
            ScaleOperator op = scaleOperator("gainExponent", bad);
            OperatorSchedule schedule = schedule();
            assertThrows(IllegalArgumentException.class, () -> schedule.addOperator(op));
        }
        // ... also with auto-tuning off, where calcDelta never looks at it
        ScaleOperator op = scaleOperator("gainExponent", 2.0);
        OperatorSchedule noTuning = schedule("autoOptimize", false);
        assertThrows(IllegalArgumentException.class, () -> noTuning.addOperator(op));
    }

    @Test
    public void operatorBoundaryExponentsAreAccepted() {
        // 1 and 0.5 are valid (0.5 only warns); tiny exponents are valid too
        for (double e : new double[] {1.0, 0.5, 1e-6}) {
            ScaleOperator op = scaleOperator("gainExponent", e);
            assertEquals(expected(op, e), delta(schedule(), op), 1e-15);
        }
    }
}
