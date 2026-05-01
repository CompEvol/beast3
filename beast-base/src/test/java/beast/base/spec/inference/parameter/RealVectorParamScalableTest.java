package beast.base.spec.inference.parameter;

import beast.base.inference.ScalableContractTest;
import beast.base.spec.domain.PositiveReal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the {@link beast.base.inference.Scalable} contract on
 * {@link RealVectorParam}.
 */
public class RealVectorParamScalableTest {

    @Test
    void contractHoldsForPositiveVector() {
        ScalableContractTest.assertContractAcrossScales(
                () -> {
                    RealVectorParam<?> p = new RealVectorParam<>(
                            new double[] { 1.0, 2.5, 3.7, 0.4 }, PositiveReal.INSTANCE);
                    p.initAndValidate();
                    return p;
                },
                this::assertSameVectorState
        );
    }

    @Test
    void contractHoldsForVectorWithMixedMagnitudes() {
        ScalableContractTest.assertContractAcrossScales(
                () -> {
                    RealVectorParam<?> p = new RealVectorParam<>(
                            new double[] { 0.001, 100.0, 5.5 }, PositiveReal.INSTANCE);
                    p.initAndValidate();
                    return p;
                },
                this::assertSameVectorState
        );
    }

    private void assertSameVectorState(RealVectorParam<?> a, RealVectorParam<?> b) {
        assertEquals(a.size(), b.size(), "Vector sizes should match");
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i), b.get(i), ScalableContractTest.EPSILON,
                    "Element " + i + " should match across scale and set+get×s paths");
        }
    }
}
