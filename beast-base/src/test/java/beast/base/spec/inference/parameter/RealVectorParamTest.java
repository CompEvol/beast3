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

    //TODO matrix

//    final String spNames = "sp1 sp2 sp3 sp4 sp5 sp6 sp7 sp8 sp9 sp10";
//    final String spNames2 = "sp11 sp12 sp13 sp14 sp15 sp16 sp17 sp18 sp19 sp20";
//
//    // each line is a species, each column a trait
//    final List<Double> twoTraitsValues =  Arrays.asList(
//            0.326278727608277, 1.8164550628074,
//            -0.370085503473201, 0.665116986641999,
//            1.17377224776421, 3.59440970719762,
//            3.38137444987329, -0.187743059073837,
//            -1.64759474375234, -2.19534387982435,
//            -3.22668212260941, -1.71183724870188,
//            1.81925405275285, -0.428821390843389,
//            4.22298205455098, 1.51483058860744,
//            3.63674837097173, 3.68456953445085,
//            -0.743303344769609, 1.10602125889508
//    );
//
//    /*
//     * This test checks whether we get all the trait values for two species.
//     * For 2D matrix, keys must either have the same length as dimension or the number of rows.
//     */
//    @Test
//    public void testKeysTwoColumns () {
//
//        final int colCount = 2;
//        RealParameter twoCols = new RealParameter();
//        twoCols.initByName("value", twoTraitsValues, "keys", spNames, "minordimension", colCount);
//
//        assertEquals(twoCols.getDimension()/colCount, twoCols.getKeysList().size());
//        assertArrayEquals(twoCols.getRowValues("sp1"), new Double[] { 0.326278727608277, 1.8164550628074 });
//        assertArrayEquals(twoCols.getRowValues("sp8"), new Double[] { 4.22298205455098, 1.51483058860744 });
//    }
//
//    /**
//     * For 1D array, keys must have the same length as dimension
//     */
//    @Test
//    public void testKeys1DArray () {
//
//        RealParameter oneTraits = new RealParameter();
//        // pretend to be 1d array now
//        oneTraits.initByName("value", twoTraitsValues, "keys", spNames+" "+spNames2);
//
//        assertEquals(oneTraits.getDimension(), oneTraits.getKeysList().size());
//        // 1d array now, so values positions are diff
//        assertArrayEquals(oneTraits.getRowValues("sp1"), new Double[] { 0.326278727608277 });
//        assertArrayEquals(oneTraits.getRowValues("sp8"), new Double[] { -0.187743059073837 });
//        assertArrayEquals(oneTraits.getRowValues("sp11"), new Double[] { -3.22668212260941 });
//        assertArrayEquals(oneTraits.getRowValues("sp19"), new Double[] { -0.743303344769609 });
//    }
//
}
