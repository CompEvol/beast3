package beast.base.spec.inference.distribution;

import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.IntSimplexParam;
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
    public void testIntSimplexWithWrongSumGivesSum() {
        IntSimplexParam<NonNegativeInt> groupSizes = new IntSimplexParam<>(new int[] {1, 2, 4}, NonNegativeInt.INSTANCE, 7);
        groupSizes.setID("groupSizes");
        groupSizes.set(2, 3); // every value still in the domain, but the sum is 6
        IID<?, ?, ?> iid = new IID<>();
        iid.paramInput.setValue(groupSizes, iid);
        iid.distInput.setValue(new Poisson(null, new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE)), iid);

        String message = assertThrows(IllegalArgumentException.class, iid::initAndValidate).getMessage();

        assertTrue(message.contains("groupSizes"), message);
        assertTrue(message.contains("The values must sum to 7, but they sum to 6."), message);
    }

    /** a simplex created with a wrong sum fails in its own initAndValidate(), and that message gives the reason too */
    @Test
    public void testSimplexCreatedWithWrongSumGivesSum() {
        String message = assertThrows(IllegalArgumentException.class,
                () -> new SimplexParam(new double[] {0.15, 0.25, 0.25, 0.25})).getMessage();
        assertTrue(message.contains("The values must sum to 1, but they sum to 0.9"), message);
        assertFalse(message.contains("range"), "every value is in range, so no range should be given: " + message);

        message = assertThrows(IllegalArgumentException.class,
                () -> new IntSimplexParam<>(new int[] {1, 2, 3}, NonNegativeInt.INSTANCE, 7)).getMessage();
        assertTrue(message.contains("The values must sum to 7, but they sum to 6."), message);
    }

    @Test
    public void testVectorCreatedWithElementOutsideRangeNamesElement() {
        String message = assertThrows(IllegalArgumentException.class,
                () -> new RealVectorParam<>(new double[] {0.5, -1.0}, PositiveReal.INSTANCE)).getMessage();
        assertTrue(message.contains("Element 2 (-1.0) is not in the domain PositiveReal"), message);
    }

    /** #332: a value in the domain but outside a prior's bounds says so, and names the prior */
    @Test
    public void testSetOutsidePriorBoundsNamesBoundsAndPrior() {
        RealScalarParam<PositiveReal> shape = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
        shape.setID("gammaShape");
        Uniform prior = new Uniform(null,
                new RealScalarParam<>(0.1, Real.INSTANCE),
                new RealScalarParam<>(5.0, Real.INSTANCE));
        prior.setID("GammaShapePrior");
        prior.paramInput.setValue(shape, prior);
        prior.initAndValidate();

        String message = assertThrows(IllegalArgumentException.class, () -> shape.set(5.1)).getMessage();
        assertEquals("gammaShape = 5.1 is outside the allowed range [0.1,5.0], set by GammaShapePrior", message);

        message = assertThrows(IllegalArgumentException.class, () -> shape.set(-1.0)).getMessage();
        assertEquals("gammaShape = -1.0 is not in the domain PositiveReal", message);

        shape.set(4.9); // inside both
        assertEquals(4.9, shape.get());
    }

    /** #332: without a prior that narrows it, a value outside the domain names the domain's simple name */
    @Test
    public void testDomainFailureUsesSimpleName() {
        IntScalarParam<PositiveInt> count = new IntScalarParam<>(1, PositiveInt.INSTANCE);
        count.setID("count");
        String message = assertThrows(IllegalArgumentException.class, () -> count.set(0)).getMessage();
        assertEquals("count = 0 is not in the domain PositiveInt", message);

        message = assertThrows(IllegalArgumentException.class,
                () -> new RealScalarParam<>(-2.0, PositiveReal.INSTANCE)).getMessage();
        assertEquals("the parameter = -2.0 is not in the domain PositiveReal", message);
    }

    /** #332: a vector names the element */
    @Test
    public void testVectorSetNamesElement() {
        RealVectorParam<PositiveReal> rates = new RealVectorParam<>(new double[] {0.5, 1.5}, PositiveReal.INSTANCE);
        rates.setID("rates");
        String message = assertThrows(IllegalArgumentException.class, () -> rates.set(1, -3.0)).getMessage();
        assertEquals("rates element 2 = -3.0 is not in the domain PositiveReal", message);
    }

    @Test
    public void testValidValueHasNoReason() {
        assertNull(new RealScalarParam<>(0.5, Real.INSTANCE).whyNotValid());
        assertNull(new SimplexParam(new double[] {0.25, 0.25, 0.25, 0.25}).whyNotValid());
        assertNull(new IntSimplexParam<>(new int[] {1, 2, 4}, NonNegativeInt.INSTANCE, 7).whyNotValid());
    }
}
