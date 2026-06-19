package beast.base.spec.inference.parameter;

import beast.base.spec.domain.Int;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ParameterUtils#parseParameter} and {@link ParameterUtils#paramToString},
 * covering the full round-trip for all supported parameter types and verifying that
 * legacy BEAST2 state files with explicit bounds are rejected.
 */
public class ParameterUtilsTest {

    // ------------------------------------------------------------------ helpers

    private static org.w3c.dom.Node createNode(String id, String textContent) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element el = doc.createElement("stateNode");
        el.setAttribute("id", id);
        el.setTextContent(textContent);
        return el;
    }

    // ------------------------------------------------------------------ scalar round-trips

    @Test
    void testRealScalarRoundTrip() throws Exception {
        RealScalarParam<PositiveReal> src = new RealScalarParam<>(1.5, PositiveReal.INSTANCE);
        src.setID("kappa");

        String str = src.toString();
        assertEquals("kappa: 1.5", str);

        RealScalarParam<PositiveReal> target = new RealScalarParam<>(0.1, PositiveReal.INSTANCE);
        ParameterUtils.parseParameter(createNode("kappa", str), target);

        assertEquals(1.5, target.get(), 1e-12);
        assertEquals("kappa", target.getID());
    }

    @Test
    void testIntScalarRoundTrip() throws Exception {
        IntScalarParam<Int> src = new IntScalarParam<>(7, Int.INSTANCE);
        src.setID("popSize");

        String str = src.toString();
        assertEquals("popSize: 7", str);

        IntScalarParam<Int> target = new IntScalarParam<>(1, Int.INSTANCE);
        ParameterUtils.parseParameter(createNode("popSize", str), target);

        assertEquals(7, target.get());
        assertEquals("popSize", target.getID());
    }

    @Test
    void testBoolScalarRoundTrip() throws Exception {
        BoolScalarParam src = new BoolScalarParam(true);
        src.setID("isEstimated");

        String str = src.toString();
        assertEquals("isEstimated: true", str);

        BoolScalarParam target = new BoolScalarParam(false);
        ParameterUtils.parseParameter(createNode("isEstimated", str), target);

        assertTrue(target.get());
        assertEquals("isEstimated", target.getID());
    }

    // ------------------------------------------------------------------ vector round-trips

    @Test
    void testRealVectorRoundTrip() throws Exception {
        RealVectorParam<Real> src = new RealVectorParam<>(new double[]{0.1, 0.2, 0.3, 0.4}, Real.INSTANCE);
        src.setID("freqs");

        String str = src.toString();
        // shape must appear so parseParameter can validate element count
        assertTrue(str.startsWith("freqs{4}: "), "Expected 'freqs{4}: ...' but was: " + str);

        RealVectorParam<Real> target = new RealVectorParam<>(new double[]{0.25, 0.25, 0.25, 0.25}, Real.INSTANCE);
        ParameterUtils.parseParameter(createNode("freqs", str), target);

        assertEquals(0.1, target.get(0), 1e-12);
        assertEquals(0.2, target.get(1), 1e-12);
        assertEquals(0.3, target.get(2), 1e-12);
        assertEquals(0.4, target.get(3), 1e-12);
        assertEquals("freqs", target.getID());
    }

    @Test
    void testIntVectorRoundTrip() throws Exception {
        IntVectorParam<Int> src = new IntVectorParam<>(new int[]{1, 2, 3}, Int.INSTANCE);
        src.setID("counts");

        String str = src.toString();
        assertTrue(str.startsWith("counts{3}: "), "Expected 'counts{3}: ...' but was: " + str);

        IntVectorParam<Int> target = new IntVectorParam<>(new int[]{0, 0, 0}, Int.INSTANCE);
        ParameterUtils.parseParameter(createNode("counts", str), target);

        assertEquals(1, target.get(0));
        assertEquals(2, target.get(1));
        assertEquals(3, target.get(2));
        assertEquals("counts", target.getID());
    }

    @Test
    void testBoolVectorRoundTrip() throws Exception {
        BoolVectorParam src = new BoolVectorParam(new boolean[]{true, false, true});
        src.setID("isSelected");

        String str = src.toString();
        assertTrue(str.startsWith("isSelected{3}: "), "Expected 'isSelected{3}: ...' but was: " + str);

        BoolVectorParam target = new BoolVectorParam(new boolean[]{false, false, false});
        ParameterUtils.parseParameter(createNode("isSelected", str), target);

        assertTrue(target.get(0));
        assertFalse(target.get(1));
        assertTrue(target.get(2));
        assertEquals("isSelected", target.getID());
    }

    // ------------------------------------------------------------------ legacy bounds rejection

    @Test
    void testLegacyScalarBoundsThrows() throws Exception {
        // BEAST2 format: explicit bounds in braces
        String legacy = "kappa{[0.0,Infinity]}: 1.5";
        RealScalarParam<PositiveReal> param = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ParameterUtils.parseParameter(createNode("kappa", legacy), param));
        assertTrue(ex.getMessage().contains("explicit bounds"));
        assertTrue(ex.getMessage().contains("prior distribution"));
    }

    @Test
    void testLegacyVectorBoundsThrows() throws Exception {
        // BEAST2 format: shape + explicit bounds
        String legacy = "freqs{4, [0.0,1.0]}: 0.25 0.25 0.25 0.25";
        RealVectorParam<Real> param = new RealVectorParam<>(new double[]{0.25, 0.25, 0.25, 0.25}, Real.INSTANCE);
        assertThrows(IllegalArgumentException.class,
                () -> ParameterUtils.parseParameter(createNode("freqs", legacy), param));
    }

    // ------------------------------------------------------------------ paramToString format

    @Test
    void testParamToStringScalarHasNoBraces() {
        // Scalars must not emit {}: the shape is implicit (rank 0)
        RealScalarParam<Real> param = new RealScalarParam<>(2.5, Real.INSTANCE);
        param.setID("mu");
        assertFalse(param.toString().contains("{"), "Scalar toString must not contain braces");
    }

    @Test
    void testParamToStringVectorHasShapeNoBoundsComma() {
        // Vectors must emit {N} — no trailing comma from the old BoundedParam format
        RealVectorParam<Real> param = new RealVectorParam<>(new double[]{1.0, 2.0}, Real.INSTANCE);
        param.setID("rates");
        String s = param.toString();
        assertTrue(s.contains("{2}"), "Vector toString must contain '{2}', got: " + s);
        assertFalse(s.contains("{2,"), "Vector toString must not contain legacy '{2,' format, got: " + s);
    }
}
