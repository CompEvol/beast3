package beast.base.spec.statistic;

import beast.base.core.BEASTInterface;
import beast.base.parser.XMLParser;
import beast.base.parser.XMLParserException;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.util.RPNcalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RPNCalculatorTest {

	
	@Test
	public void testRPNDivide() {
        RealScalarParam<Real> p1 = new RealScalarParam<>(2.5, Real.INSTANCE);
		p1.setID("p1");
        RealScalarParam<Real> p2 = new RealScalarParam<>(5, Real.INSTANCE);
		p2.setID("p2");
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", p1, "parameter", p2, "expression", "p2 p1 /");
		
		Double result = calculator.get();
		assertEquals(2.0, result, 1e-16);
	}

	@Test
	public void testRPNDivideSwitchID() {
        RealScalarParam<Real> p1 = new RealScalarParam<>(2.5, Real.INSTANCE);
		p1.setID("p2");
        RealScalarParam<Real> p2 = new RealScalarParam<>(5, Real.INSTANCE);
		p2.setID("p1");
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", p1, "parameter", p2, "expression", "p2 p1 /");

		double result = calculator.get();
		assertEquals(0.5, result, 1e-16);
	}

	@Test
	public void testRPNDivideMultiDim() {
        RealVectorParam<Real> p1 = new RealVectorParam<>(new double[]{2.5,1.0,5}, Real.INSTANCE);
		p1.setID("p1");
        RealVectorParam<Real> p2 = new RealVectorParam<>(new double[]{5,1.0,2.5}, Real.INSTANCE);
		p2.setID("p2");
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", p1, "parameter", p2, "expression", "p2 p1 /");

		double result = calculator.get();
		assertEquals(2.0, result, 1e-16);
		result = calculator.get(1);
		assertEquals(1.0, result, 1e-16);
		result = calculator.get(2);
		assertEquals(0.5, result, 1e-16);
	}
	
	@Test
	public void testRPNDivideSpaceInID() {
        RealScalarParam<Real> p1 = new RealScalarParam<>(2.5, Real.INSTANCE);
        p1.setID("p 1");
        RealScalarParam<Real> p2 = new RealScalarParam<>(5, Real.INSTANCE);
        p2.setID("p 2");

		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", p1, "parameter", p2, "expression", "x2 x1 /", "argnames", "x1,x2");

		double result = calculator.get();
		assertEquals(2.0, result, 1e-16);
	}


	@Test
	public void testRPNDivideSpaceInIDMultiDim() {
        RealVectorParam<Real> p1 = new RealVectorParam<>(new double[]{2.5,1.0,5}, Real.INSTANCE);
        p1.setID("p 1");
        RealVectorParam<Real> p2 = new RealVectorParam<>(new double[]{5,1.0,2.5}, Real.INSTANCE);
        p2.setID("p 2");
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", p1, "parameter", p2, "expression", "x2 x1 /", "argnames", "x1,x2");

		double result = calculator.get();
		assertEquals(2.0, result, 1e-16);
		result = calculator.get(1);
		assertEquals(1.0, result, 1e-16);
		result = calculator.get(2);
		assertEquals(0.5, result, 1e-16);
	}


	@Test
	public void testRPNMultiplyBoolean() {
		BoolVectorParam p1 = new BoolVectorParam(new boolean[]{false, true, false, true});
		p1.setID("p1");
        RealVectorParam<Real> p2 = new RealVectorParam<>(new double[]{5,6,7,8}, Real.INSTANCE);
		p2.setID("p2");
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", p1, "parameter", p2, "expression", "p2 p1 *");

		double [] result = calculator.getDoubleValues();
		assertEquals(0.0, result[0], 1e-16);
		assertEquals(6.0, result[1], 1e-16);
		assertEquals(0.0, result[2], 1e-16);
		assertEquals(8.0, result[3], 1e-16);
	}

	@Test
	public void testRPNXMLParser() throws XMLParserException {
		XMLParser parser = new XMLParser();
		String xml = 
				"<parameter id='p1' spec='beast.base.spec.inference.parameter.BoolVectorParam' value='0 1 0 1'/>\n" +
			    "<parameter id='p2' value='5. 6. 7. 8.' spec='beast.base.spec.inference.parameter.RealVectorParam'/>\n" +
			    "<calculator id='calculator' spec='beast.base.spec.inference.util.RPNcalculator' expression='p1 p2 *'>\n" +
			    "    <parameter idref='p1'/>\n" +
			    "    <parameter idref='p2'/>\n" +
			    "</calculator>"
		;
				
		BEASTInterface o = parser.parseBareFragment(xml, true);
		RPNcalculator calculator = (RPNcalculator) o;

		double [] result = calculator.getDoubleValues();
		assertEquals(0.0, result[0], 1e-16);
		assertEquals(6.0, result[1], 1e-16);
		assertEquals(0.0, result[2], 1e-16);
		assertEquals(8.0, result[3], 1e-16);
	}


}


