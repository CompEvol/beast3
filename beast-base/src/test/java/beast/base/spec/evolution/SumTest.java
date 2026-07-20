package beast.base.spec.evolution;

import beast.base.spec.domain.Int;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class SumTest {

    static final double DELTA = 1e-10;

    @Test
    public void testSumSingleRealArg() {
        RealVectorParam<Real> p1 = new RealVectorParam<>(new double[]{1.0, 2.0}, Real.INSTANCE);
        Sum sum = new Sum();
        sum.initByName("arg", p1);
        assertEquals(3.0, sum.get(), DELTA);
    }

    @Test
    public void testSumMultipleRealArgs() {
        RealVectorParam<Real> p1 = new RealVectorParam<>(new double[]{1.0, 2.0}, Real.INSTANCE);
        RealVectorParam<Real> p2 = new RealVectorParam<>(new double[]{2.0, 2.5}, Real.INSTANCE);
        Sum sum = new Sum();
        sum.initByName("arg", p1, "arg", p2);
        assertEquals(7.5, sum.get(), DELTA);
    }

    @Test
    public void testSumRepeatedRealArg() {
        RealVectorParam<Real> p1 = new RealVectorParam<>(new double[]{1.0, 2.0}, Real.INSTANCE);
        Sum sum = new Sum();
        sum.initByName("arg", p1, "arg", p1);
        assertEquals(6.0, sum.get(), DELTA);
    }

    @Test
    public void testSumIntegerArg() {
        IntVectorParam<Int> p3 = new IntVectorParam<>(new int[]{1, 2, 5}, Int.INSTANCE);
        Sum sum = new Sum();
        sum.initByName("arg", p3);
        assertEquals(8.0, sum.get(), DELTA);
    }

    @Test
    public void testSumBooleanArg() {
        BoolVectorParam p4 = new BoolVectorParam(new boolean[]{true, false, false, true, true});
        Sum sum = new Sum();
        sum.initByName("arg", p4);
        assertEquals(3.0, sum.get(), DELTA);
    }

    @Test
    public void testSumBooleanAndIntegerArgs() {
        BoolVectorParam p4 = new BoolVectorParam(new boolean[]{true, false, false, true, true});
        IntVectorParam<Int> p3 = new IntVectorParam<>(new int[]{1, 2, 5}, Int.INSTANCE);
        Sum sum = new Sum();
        sum.initByName("arg", p4, "arg", p3);
        assertEquals(11.0, sum.get(), DELTA);
    }

    @Test
    public void testSumBooleanAndRealArgs() {
        RealVectorParam<Real> p1 = new RealVectorParam<>(new double[]{1.0, 2.0}, Real.INSTANCE);
        BoolVectorParam p4 = new BoolVectorParam(new boolean[]{true, false, false, true, true});
        Sum sum = new Sum();
        sum.initByName("arg", p1, "arg", p4);
        assertEquals(6.0, sum.get(), DELTA);
    }
}
