package beast.base.spec.inference.parameter;


import beast.base.spec.domain.PositiveReal;
import org.junit.jupiter.api.Test;

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

        //TODO ?, no exception here
        try {
            parameter.set(2, 2.0); // this will throw an exception
            assertNotSame(parameter.get(0), parameter.get(2));
        } catch (Exception e) {
            // setValue is not allowed for StateNode not in State
        }

    }

    @Test
    public void testInitAndValidate() {

        double[] x = {1.0, 2.0, 3.0, 2.0, 4.0, 5.5};
        try {
            RealVectorParam parameter = new RealVectorParam(x, PositiveReal.INSTANCE, 2.0, 6.0);
            assertEquals(6, parameter.size());

            fail("Expected IllegalArgumentException not thrown");
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            assertTrue(message.contains("not valid") & message.contains("or bounds"), ex.getMessage());
        }

        x = new double[]{-1.0, 2.0, 3.0, 2.0, 4.0, 5.5};

        try {
            // if using constructor, validation is in initAndValidate()
            RealVectorParam parameter = new RealVectorParam(x, PositiveReal.INSTANCE);
            fail("Expected IllegalArgumentException not thrown");
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            assertTrue(message.contains("not valid for domain") & message.contains("PositiveReal"), ex.getMessage());
        }
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
