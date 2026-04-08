package beast.base.spec.inference.parameter;

import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.domain.UnitInterval;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VectorElementTest {

    @Test
    void testGet() {
        RealVectorParam<PositiveReal> vector = new RealVectorParam<>(
                new double[]{1.5, 2.5, 3.5}, PositiveReal.INSTANCE);
        vector.setID("rates");

        VectorElement<PositiveReal> elem0 = new VectorElement<>(vector, 0);
        VectorElement<PositiveReal> elem1 = new VectorElement<>(vector, 1);
        VectorElement<PositiveReal> elem2 = new VectorElement<>(vector, 2);

        assertEquals(1.5, elem0.get());
        assertEquals(2.5, elem1.get());
        assertEquals(3.5, elem2.get());
    }

    @Test
    void testDomainInherited() {
        RealVectorParam<UnitInterval> vector = new RealVectorParam<>(
                new double[]{0.25, 0.75}, UnitInterval.INSTANCE);
        vector.setID("freqs");

        VectorElement<UnitInterval> elem = new VectorElement<>(vector, 0);

        assertSame(UnitInterval.INSTANCE, elem.getDomain());
    }

    @Test
    void testReflectsVectorChanges() {
        RealVectorParam<Real> vector = new RealVectorParam<>(
                new double[]{1.0, 2.0}, Real.INSTANCE);
        vector.setID("param");

        VectorElement<Real> elem = new VectorElement<>(vector, 1);
        assertEquals(2.0, elem.get());

        vector.set(1, 5.0);
        assertEquals(5.0, elem.get());
    }

    @Test
    void testIndexOutOfBounds() {
        RealVectorParam<PositiveReal> vector = new RealVectorParam<>(
                new double[]{1.0, 2.0}, PositiveReal.INSTANCE);
        vector.setID("param");

        assertThrows(RuntimeException.class,
                () -> new VectorElement<>(vector, 2));
        assertThrows(RuntimeException.class,
                () -> new VectorElement<>(vector, -1));
    }

    @Test
    void testInitByName() {
        RealVectorParam<PositiveReal> vector = new RealVectorParam<>(
                new double[]{10.0, 20.0, 30.0}, PositiveReal.INSTANCE);
        vector.setID("v");

        VectorElement<PositiveReal> elem = new VectorElement<>();
        elem.initByName("vector", vector, "index", 2);

        assertEquals(30.0, elem.get());
        assertSame(PositiveReal.INSTANCE, elem.getDomain());
    }
}
