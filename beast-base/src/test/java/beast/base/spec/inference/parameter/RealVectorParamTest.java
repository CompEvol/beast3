package beast.base.spec.inference.parameter;


import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.distribution.IID;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.distribution.TruncatedReal;
import beast.base.spec.inference.distribution.Uniform;
import org.junit.jupiter.api.Test;
import test.beast.BEASTTestCase;

import static org.junit.jupiter.api.Assertions.*;

public class RealVectorParamTest {

    @Test
    public void testValuesAndDim() {
        RealVectorParam parameter = new RealVectorParam();
        parameter.initByName("value", "1.27 1.9", "domain", PositiveReal.INSTANCE);
        assertEquals(2, parameter.size());
        parameter.setDimension(5);
        // null{5, [0.0,Infinity]}: 1.27 1.9 1.27 1.9 1.27
        assertEquals(5, parameter.size());
        assertEquals(parameter.get(0), parameter.get(2));
        assertEquals(parameter.get(0), parameter.get(4));
        assertEquals(parameter.get(1), parameter.get(3));
        assertNotSame(parameter.get(0), parameter.get(1));

        try {
            parameter.set(2, 0.0); // domain checking
            fail("Expected IllegalArgumentException not thrown");
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            assertTrue(message.contains("not valid for domain") & message.contains("PositiveReal"), ex.getMessage());
        }

        parameter.set(2, 2.0); // this will throw an exception
        assertNotSame(parameter.get(0), parameter.get(2));

    }


    @Test
    public void testValuesAndDimInit() {
        RealVectorParam parameter = new RealVectorParam();
        parameter.initByName("dimension", 5, "value", "1.27 1.9", "domain", PositiveReal.INSTANCE);
        // null{5, [0.0,Infinity]}: 1.27 1.9 1.27 1.9 1.27
        assertEquals(5, parameter.size());
        assertEquals(1.27, parameter.get(0));
        assertEquals(1.9, parameter.get(1));
        assertEquals(1.27, parameter.get(2));
        assertEquals(1.9, parameter.get(3));
        assertEquals(1.27, parameter.get(4));

        final double[] x = new double[]{1, 2};
        parameter = new RealVectorParam(5, x, PositiveReal.INSTANCE);
        assertEquals(5, parameter.size());
        assertEquals(1, parameter.get(0));
        assertEquals(2, parameter.get(1));
        assertEquals(1, parameter.get(2));
        assertEquals(2, parameter.get(3));
        assertEquals(1, parameter.get(4));

        // freqs
        parameter = new RealVectorParam();
        parameter.initByName("dimension", 4, "value", "0.25", "domain", UnitInterval.INSTANCE);
        assertEquals(4, parameter.size());
        for (int i = 0; i < parameter.size(); i++) {
            assertEquals(0.25, parameter.get(i));
        }
    }


    @Test
    public void testInitAndValidate() {

        double[] x = new double[]{-1.0, 2.0, 3.0, 2.0, 4.0, 5.5};

        try {
            // if using constructor, validation is in initAndValidate()
            RealVectorParam parameter = new RealVectorParam(x, PositiveReal.INSTANCE);
            // Exception throws after the x is set
            assertEquals(6, parameter.size());

            fail("Expected IllegalArgumentException not thrown");
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            assertTrue(message.contains("not valid for domain") & message.contains("PositiveReal"), ex.getMessage());
        }
    }

    @Test
    void testBounds() {
        RealVectorParam param = new RealVectorParam(new double[]{1.0,1.0}, PositiveReal.INSTANCE);
        param.setID("param");

        Normal normal = new Normal(null, new RealScalarParam(0, Real.INSTANCE),
                new RealScalarParam(1, PositiveReal.INSTANCE));
        normal.setID("normal");
        IID iid = new IID(param, normal);
        iid.setID("iid");

        assertEquals(0.0, param.getLower(), BEASTTestCase.PRECISION);
        assertEquals(Double.POSITIVE_INFINITY, param.getUpper(), BEASTTestCase.PRECISION);

        // Note IID bounds are Normal bounds, which is diff to param bounds
        assertEquals(Double.NEGATIVE_INFINITY, (Double) iid.getLowerBoundOfParameter(), BEASTTestCase.PRECISION);
        assertEquals(Double.POSITIVE_INFINITY, (Double) iid.getUpperBoundOfParameter(), BEASTTestCase.PRECISION);
    }

    @Test
    void testBounds2() {
        RealVectorParam param = new RealVectorParam(new double[]{1.0,1.0}, PositiveReal.INSTANCE);
        param.setID("param");

        // base dist has no param
        Normal normal = new Normal(null, new RealScalarParam(0, Real.INSTANCE),
                new RealScalarParam(1, PositiveReal.INSTANCE));
        normal.setID("normal");

        // now wrap the Normal in a TruncatedRealDistribution to specify narrower bounds
        TruncatedReal truncated = new TruncatedReal(normal, 1.0, 2.0);
        truncated.setID("truncated");

        IID iid = new IID(param, truncated);
        iid.setID("iid");

        assertEquals(1.0, param.getLower(), BEASTTestCase.PRECISION);
        assertEquals(2.0, param.getUpper(), BEASTTestCase.PRECISION);

        assertEquals(1.0, truncated.getLowerBoundOfParameter(), BEASTTestCase.PRECISION);
        assertEquals(2.0, truncated.getUpperBoundOfParameter(), BEASTTestCase.PRECISION);

        assertEquals(1.0, (Double) iid.getLowerBoundOfParameter(), BEASTTestCase.PRECISION);
        assertEquals(2.0, (Double) iid.getUpperBoundOfParameter(), BEASTTestCase.PRECISION);
    }

    @Test
    void testBounds3() {
        RealVectorParam param = new RealVectorParam(new double[]{1.0,1.0}, PositiveReal.INSTANCE);
        param.setID("param");

        // base dist has no param
        Uniform uniform = new Uniform(null,
                new RealScalarParam<>(1.0, Real.INSTANCE),
                new RealScalarParam<>(2.0, Real.INSTANCE));

        IID iid = new IID(param, uniform);
        iid.setID("iid");

        assertEquals(1.0, param.getLower(), BEASTTestCase.PRECISION);
        assertEquals(2.0, param.getUpper(), BEASTTestCase.PRECISION);

        assertEquals(1.0, uniform.getLowerBoundOfParameter(), BEASTTestCase.PRECISION);
        assertEquals(2.0, uniform.getUpperBoundOfParameter(), BEASTTestCase.PRECISION);

        assertEquals(1.0, (Double) iid.getLowerBoundOfParameter(), BEASTTestCase.PRECISION);
        assertEquals(2.0, (Double) iid.getUpperBoundOfParameter(), BEASTTestCase.PRECISION);
    }


    //*** test keys ***//

    @Test
    public void testGetKey() {
        RealVectorParam keyParam = new RealVectorParam();
        // pretend to be 1d array now
        keyParam.initByName("value", "3.0 2.0 1.0", "domain", PositiveReal.INSTANCE);

        // the i'th value
        assertEquals("1", keyParam.getKey(0));
        assertEquals("3", keyParam.getKey(2));

        try {
            keyParam.getKey(3);
        } catch (IllegalArgumentException ex) {
            assertEquals("Invalid index 3", ex.getMessage());
        }

    }

    //*** shape (matrix) tests ***//

    @Test
    public void testShapeBasic() {
        // 3x4 matrix, 12 values, row-major
        RealVectorParam<Real> param = new RealVectorParam<>();
        param.initByName("value", "1 2 3 4 5 6 7 8 9 10 11 12",
                "shape", "3 4", "domain", Real.INSTANCE);

        assertEquals(12, param.size());
        assertEquals(2, param.rank());
        assertArrayEquals(new int[]{3, 4}, param.shape());
        assertEquals(3, param.nrows());
        assertEquals(4, param.ncols());

        // row-major: values[row * 4 + col]
        assertEquals(1.0, param.get(0, 0));  // row 0, col 0
        assertEquals(4.0, param.get(0, 3));  // row 0, col 3
        assertEquals(5.0, param.get(1, 0));  // row 1, col 0
        assertEquals(12.0, param.get(2, 3)); // row 2, col 3

        // flat access still works
        assertEquals(1.0, param.get(0));
        assertEquals(12.0, param.get(11));
    }

    @Test
    public void testShapeDefault() {
        // no shape = flat vector
        RealVectorParam<Real> param = new RealVectorParam<>(new double[]{1, 2, 3}, Real.INSTANCE);

        assertEquals(1, param.rank());
        assertArrayEquals(new int[]{3}, param.shape());
        assertEquals(3, param.nrows());
        assertEquals(1, param.ncols());
    }

    @Test
    public void testShapeMismatch() {
        // product of shape (2*3=6) != dimension (5)
        try {
            RealVectorParam<Real> param = new RealVectorParam<>();
            param.initByName("value", "1 2 3 4 5", "shape", "2 3", "domain", Real.INSTANCE);
            fail("Expected exception for shape/dimension mismatch");
        } catch (RuntimeException ex) {
            assertTrue(ex.getMessage().contains("must equal dimension"));
        }
    }

    @Test
    public void testShapeWrongIndexCount() {
        RealVectorParam<Real> param = new RealVectorParam<>();
        param.initByName("value", "1 2 3 4 5 6", "shape", "2 3", "domain", Real.INSTANCE);

        // 3 indices for a 2D shape
        try {
            param.get(0, 0, 0);
            fail("Expected IndexOutOfBoundsException for wrong index count");
        } catch (IndexOutOfBoundsException ex) {
            assertTrue(ex.getMessage().contains("Expected 2 indices, got 3"));
        }
    }

}
