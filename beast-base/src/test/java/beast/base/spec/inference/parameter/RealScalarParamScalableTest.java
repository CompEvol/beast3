package beast.base.spec.inference.parameter;

import beast.base.inference.ScalableContractTest;
import beast.base.spec.domain.PositiveReal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the {@link beast.base.inference.Scalable} contract on
 * {@link RealScalarParam}.
 */
public class RealScalarParamScalableTest {

    @Test
    void contractHoldsForPositiveScalar() {
        ScalableContractTest.assertContractAcrossScales(
                () -> {
                    RealScalarParam<?> p = new RealScalarParam<>(1.5, PositiveReal.INSTANCE);
                    p.initAndValidate();
                    return p;
                },
                (a, b) -> assertEquals(a.get(), b.get(), ScalableContractTest.EPSILON,
                        "RealScalarParam internal value should match across scale and set+get×s paths")
        );
    }

    @Test
    void contractHoldsForLargerInitialValue() {
        ScalableContractTest.assertContractAcrossScales(
                () -> {
                    RealScalarParam<?> p = new RealScalarParam<>(42.0, PositiveReal.INSTANCE);
                    p.initAndValidate();
                    return p;
                },
                (a, b) -> assertEquals(a.get(), b.get(), ScalableContractTest.EPSILON)
        );
    }
}
