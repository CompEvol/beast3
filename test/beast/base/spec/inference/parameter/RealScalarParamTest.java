package beast.base.spec.inference.parameter;

import beast.base.parser.XMLProducer;
import beast.base.spec.domain.PositiveReal;
import org.junit.jupiter.api.Test;

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
}