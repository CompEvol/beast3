package beast.base.spec.inference.distribution;

import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.SimplexParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The message for a parameter whose value a distribution does not allow must say what is
 * wrong: the allowed range only when a value is outside it, and the sum for a simplex
 * whose values are in range but do not sum to 1 (issue #253).
 */
public class InvalidValueMessageTest {

    @Test
    public void testValueOutsideRangeGivesRange() {
        RealScalarParam<Real> kappa = new RealScalarParam<>(2.0, Real.INSTANCE);
        kappa.setID("kappa");
        Uniform uniform = new Uniform(null,
                new RealScalarParam<>(0.0, Real.INSTANCE),
                new RealScalarParam<>(1.0, Real.INSTANCE));
        uniform.paramInput.setValue(kappa, uniform);

        String message = assertThrows(IllegalArgumentException.class, uniform::initAndValidate).getMessage();

        assertTrue(message.contains("kappa"), message);
        assertTrue(message.contains("It is outside the allowed range [0.0,1.0]"), message);
        assertEquals(message.indexOf("2.0"), message.lastIndexOf("2.0"), "the value should be given once: " + message);
    }

    @Test
    public void testSimplexNotSummingToOneGivesSum() {
        SimplexParam freqs = new SimplexParam(new double[] {0.25, 0.25, 0.25, 0.25});
        freqs.setID("freqs");
        freqs.set(0, 0.15); // every value still in [0,1], but the sum is 0.9
        Dirichlet dirichlet = new Dirichlet();
        dirichlet.paramInput.setValue(freqs, dirichlet);
        dirichlet.alphaInput.setValue(new RealVectorParam<>(new double[] {1, 1, 1, 1}, PositiveReal.INSTANCE), dirichlet);

        String message = assertThrows(IllegalArgumentException.class, dirichlet::initAndValidate).getMessage();

        assertTrue(message.contains("freqs"), message);
        assertTrue(message.contains("must sum to 1, but they sum to 0.9"), message);
        assertFalse(message.contains("range"), "every value is in range, so no range should be given: " + message);
    }

    @Test
    public void testVectorNamesElementOutsideRange() {
        RealVectorParam<Real> rates = new RealVectorParam<>(new double[] {0.5, 1.5}, Real.INSTANCE);
        rates.setID("rates");
        Uniform uniform = new Uniform(null,
                new RealScalarParam<>(0.0, Real.INSTANCE),
                new RealScalarParam<>(1.0, Real.INSTANCE));
        IID<?, ?, ?> iid = new IID<>();
        iid.paramInput.setValue(rates, iid);
        iid.distInput.setValue(uniform, iid);

        String message = assertThrows(IllegalArgumentException.class, iid::initAndValidate).getMessage();

        assertTrue(message.contains("Element 2 (1.5) is outside the allowed range [0.0,1.0]"), message);
    }

    @Test
    public void testValidValueHasNoReason() {
        assertNull(new RealScalarParam<>(0.5, Real.INSTANCE).whyNotValid());
        assertNull(new SimplexParam(new double[] {0.25, 0.25, 0.25, 0.25}).whyNotValid());
    }
}
