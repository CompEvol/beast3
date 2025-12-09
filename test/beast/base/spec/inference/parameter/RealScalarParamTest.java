package beast.base.spec.inference.parameter;

import beast.base.parser.XMLProducer;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.distribution.TruncatedRealDistribution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealScalarParamTest {


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

        Normal normal = new Normal(param, new RealScalarParam(0, Real.INSTANCE), new RealScalarParam(1, PositiveReal.INSTANCE));
        assertEquals(0.0, param.getLower(), 1e-10);
    	assertEquals(Double.POSITIVE_INFINITY, param.getUpper(), 1e-10);
    
        
    	// now wrap the Normal in a TruncatedRealDistribution to specify narrower bounds
    	TruncatedRealDistribution truncated = new TruncatedRealDistribution(normal, 1.0, 2.0);
    	truncated.initByName("param", param);
    	
    	assertEquals(1.0, param.getLower(), 1e-10);
    	assertEquals(2.0, param.getUpper(), 1e-10);
    }
}