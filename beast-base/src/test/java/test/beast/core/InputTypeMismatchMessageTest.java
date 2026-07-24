package test.beast.core;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import beast.base.inference.parameter.RealParameter;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.distribution.Exponential;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;

/**
 * A type mismatch has to say enough to be acted on.
 *
 * <p>The message used to read only "type mismatch for input mean", naming neither the
 * expected nor the supplied type, and the XML parser error that wraps it points at the
 * enclosing element rather than the offending child. Working out that a legacy BEAST 2
 * class had been supplied where a BEAST 3 spec one was wanted meant reading the source
 * of the class that declared the input.
 */
public class InputTypeMismatchMessageTest {

    private static String mismatchMessageFor(Object badValue) {
        Exponential exponential = new Exponential();
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> exponential.meanInput.setValue(badValue, exponential));
        return e.getMessage();
    }

    /** Supplying a legacy RealParameter where a spec RealScalar is wanted. */
    @Test
    public void testMessageNamesInputExpectedAndActual() {
        String msg = mismatchMessageFor(new RealParameter("1.0"));

        assertTrue(msg.contains("'mean'"), "should name the offending input, was: " + msg);
        assertTrue(msg.contains(Exponential.class.getName()),
                "should name the object whose input failed, was: " + msg);
        assertTrue(msg.contains("beast.base.spec.type.RealScalar"),
                "should name the expected type, was: " + msg);
        assertTrue(msg.contains(RealParameter.class.getName()),
                "should name the supplied type, was: " + msg);
    }

    /**
     * Legacy and spec classes often share a simple name, so the message has to be
     * explicit that the supplied class is the BEAST 2 one.
     */
    @Test
    public void testLegacyClassGetsMigrationHint() {
        String msg = mismatchMessageFor(new RealParameter("1.0"));

        assertTrue(msg.contains("legacy BEAST 2 class"),
                "should flag the legacy/spec confusion, was: " + msg);
        assertTrue(msg.contains("spec="),
                "should show how to declare the input explicitly, was: " + msg);
    }

    /**
     * A mismatch between two spec types -- here a vector where a scalar is wanted --
     * has nothing to do with the migration and must not get the misleading hint.
     */
    @Test
    public void testNonLegacyMismatchHasNoMigrationHint() {
        String msg = mismatchMessageFor(
                new RealVectorParam<>(new double[]{1.0, 2.0}, PositiveReal.INSTANCE));

        assertTrue(msg.contains("'mean'"), "should still name the input, was: " + msg);
        assertTrue(msg.contains(RealVectorParam.class.getName()),
                "should name the supplied type, was: " + msg);
        assertTrue(!msg.contains("legacy BEAST 2 class"),
                "should not claim a legacy class was supplied, was: " + msg);
    }

    /** A correctly typed spec parameter is still accepted. */
    @Test
    public void testSpecParameterIsAccepted() {
        Exponential exponential = new Exponential();
        exponential.meanInput.setValue(new RealScalarParam<>(1.0, PositiveReal.INSTANCE), exponential);
    }

}
