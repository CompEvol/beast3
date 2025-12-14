package beast.base.spec.inference.parameter;

import beast.base.parser.XMLProducer;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.distribution.TruncatedRealDistribution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RealScalarParamTest {


    @Test
    void testXML() {

        RealScalarParam param = new RealScalarParam(1.0, PositiveReal.INSTANCE);
        param.initAndValidate();

        XMLProducer xmlProducer = new XMLProducer();
        String xml = xmlProducer.toXML(param);

        System.out.println(xml);

        assertFalse(xml.contains("id=\"PositiveReal\""));
        assertTrue(xml.contains("domain=\"PositiveReal\""), "XML contains 'domain=\"PositiveReal\".");

    }
    
    @Test
    void testBounds() {
        RealScalarParam param = new RealScalarParam(1.0, PositiveReal.INSTANCE);
        param.setID("param");

        Normal normal = new Normal(param, new RealScalarParam(0, Real.INSTANCE), new RealScalarParam(1, PositiveReal.INSTANCE));
        normal.setID("normal");
        assertEquals(0.0, param.getLower(), 1e-10);
    	assertEquals(Double.POSITIVE_INFINITY, param.getUpper(), 1e-10);
    }
    @Test
    void testBounds2() {
        RealScalarParam param = new RealScalarParam(1.0, PositiveReal.INSTANCE);
        param.setID("param");

        // base dist has no param
        Normal normal = new Normal(null, new RealScalarParam(0, Real.INSTANCE), new RealScalarParam(1, PositiveReal.INSTANCE));
        normal.setID("normal");

        // now wrap the Normal in a TruncatedRealDistribution to specify narrower bounds
        TruncatedRealDistribution truncated = new TruncatedRealDistribution(normal, 1.0, 2.0);
        truncated.setID("truncated");
        truncated.initByName("param", param);

        assertEquals(1.0, truncated.getLower(), 1e-10);
        assertEquals(2.0, truncated.getUpper(), 1e-10);

        assertEquals(1.0, param.getLower(), 1e-10);
        assertEquals(2.0, param.getUpper(), 1e-10);
    }

}