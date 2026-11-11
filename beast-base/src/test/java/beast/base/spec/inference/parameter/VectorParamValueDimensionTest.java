package beast.base.spec.inference.parameter;

import beast.base.core.BEASTInterface;
import beast.base.spec.domain.Int;
import beast.base.spec.domain.Real;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The number of values of a vector parameter must be 1 (used for every element) or equal
 * to its dimension; dimension 1, the default, takes the number of values (issue #244).
 * Before, value="0.1 0.2" dimension="3" silently became [0.1, 0.2, 0.1].
 */
public class VectorParamValueDimensionTest {

    private static String refusal(BEASTInterface param, Object value, int dimension) {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> param.initByName("value", value, "dimension", dimension));
        return e.getMessage();
    }

    @Test
    public void testRealVectorFewerValuesThanDimensionRefused() {
        RealVectorParam<Real> param = new RealVectorParam<>();
        param.setID("x");
        String message = refusal(param, "0.1 0.2", 3);
        assertTrue(message.contains("Parameter x has 2 values, but dimension 3"), message);
    }

    @Test
    public void testRealVectorMoreValuesThanDimensionRefused() {
        String message = refusal(new RealVectorParam<Real>(), "0.1 0.2 0.3", 2);
        assertTrue(message.contains("has 3 values, but dimension 2"), message);
    }

    @Test
    public void testIntAndBoolVectorRefused() {
        assertTrue(refusal(new IntVectorParam<Int>(), "1 2", 3).contains("has 2 values, but dimension 3"));
        assertTrue(refusal(new BoolVectorParam(), "true false", 3).contains("has 2 values, but dimension 3"));
    }

    @Test
    public void testSimplexRefused() {
        assertTrue(refusal(new SimplexParam(), "0.5 0.5", 4).contains("has 2 values, but dimension 4"));
        assertTrue(refusal(new IntSimplexParam<>(), "1 2", 3).contains("has 2 values, but dimension 3"));
    }

    @Test
    public void testOneValueFillsDimension() {
        RealVectorParam<Real> real = new RealVectorParam<>();
        real.initByName("value", "0.5", "dimension", 3);
        assertEquals(List.of(0.5, 0.5, 0.5), real.getElements());

        SimplexParam simplex = new SimplexParam();
        simplex.initByName("value", "0.25", "dimension", 4);
        assertEquals(List.of(0.25, 0.25, 0.25, 0.25), simplex.getElements());

        IntVectorParam<Int> integer = new IntVectorParam<>();
        integer.initByName("value", "7", "dimension", 2);
        assertEquals(List.of(7, 7), integer.getElements());

        BoolVectorParam bool = new BoolVectorParam();
        bool.initByName("value", "true", "dimension", 2);
        assertEquals(List.of(true, true), bool.getElements());
    }

    @Test
    public void testValuesWithoutDimensionOrMatchingDimension() {
        RealVectorParam<Real> noDimension = new RealVectorParam<>();
        noDimension.initByName("value", "0.1 0.2 0.3");
        assertEquals(3, noDimension.size());

        RealVectorParam<Real> matching = new RealVectorParam<>();
        matching.initByName("value", "0.1 0.2 0.3", "dimension", 3);
        assertEquals(List.of(0.1, 0.2, 0.3), matching.getElements());
    }

    /** setDimension() must leave a parameter that initAndValidate() accepts and gives back unchanged */
    @Test
    public void testSetDimensionKeepsValueInputConsistent() {
        RealVectorParam<Real> real = new RealVectorParam<>();
        real.initByName("value", "0.1 0.2 0.3");
        real.setDimension(4);
        assertEquals(4, real.valuesInput.get().size(), "the value input follows the new dimension");
        List<Double> afterResize = real.getElements();
        real.initAndValidate();
        assertEquals(afterResize, real.getElements());

        IntVectorParam<Int> integer = new IntVectorParam<>();
        integer.initByName("value", "1 2 3");
        integer.setDimension(2);
        integer.initAndValidate();
        assertEquals(List.of(1, 2), integer.getElements());

        BoolVectorParam bool = new BoolVectorParam();
        bool.initByName("value", "true false");
        bool.setDimension(3);
        bool.initAndValidate();
        assertEquals(List.of(true, false, true), bool.getElements());
    }

    /** a single value fills any dimension, so it is kept as it is (compact XML, e.g. value="1.0" for clock rates) */
    @Test
    public void testSetDimensionKeepsSingleValue() {
        RealVectorParam<Real> rates = new RealVectorParam<>();
        rates.initByName("value", "1.0");
        rates.setDimension(5);
        assertEquals(List.of(1.0), rates.valuesInput.get());
        rates.initAndValidate();
        assertEquals(5, rates.size());
    }
}
