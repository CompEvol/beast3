package beast.base.spec.inference.distribution;


import beast.base.core.BEASTInterface;
import beast.base.parser.XMLParser;
import beast.base.parser.XMLParserException;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MeanOfParametricDistributionTest  {

	@Test
    public void testMeanOfNormal() {
		Normal normal = new Normal();
        normal.initByName("mean", new RealScalarParam<>(123, Real.INSTANCE),
                "sigma", new RealScalarParam<>(3, PositiveReal.INSTANCE));
		double mean = normal.getMean();
        assertEquals(mean, 123, 1e-10);

        normal = new Normal();
        normal.initByName("mean", new RealScalarParam<>(123, Real.INSTANCE),
                "sigma", new RealScalarParam<>(30, PositiveReal.INSTANCE));
		mean = normal.getMean();
        assertEquals(mean, 123, 1e-10);
	}
	
	@Test
    public void testMeanOfOffset() throws XMLParserException {
		Normal normal = new Normal(null, new RealScalarParam<>(123, Real.INSTANCE),
				 new RealScalarParam<>(3, PositiveReal.INSTANCE));
		
		OffsetReal offsetReal = new OffsetReal(normal, 3.0);
		
		double mean = offsetReal.getMean();
		assertEquals(mean, 126, 1e-10);
		
		
		ScalarDistribution gamma = new Gamma(null, new RealScalarParam<>(100, PositiveReal.INSTANCE), new RealScalarParam<>(10, PositiveReal.INSTANCE));
		offsetReal = new OffsetReal(gamma, 3.0);
		
		mean = offsetReal.getMean();
		assertEquals(mean, 1003, 1e-10);
		
		ScalarDistribution exp = new Exponential(null, new RealScalarParam<>(1, PositiveReal.INSTANCE));
		offsetReal = new OffsetReal(exp, 3.0);
		mean = offsetReal.getMean();
		assertEquals(mean, 4, 1e-10);

		
		// logNormal(1, 1, meanInRealSpace=true) + offset=3
		ScalarDistribution logNormal = new LogNormal(null, new RealScalarParam<>(1, Real.INSTANCE), new RealScalarParam<>(1, PositiveReal.INSTANCE), true);
		offsetReal = new OffsetReal(logNormal, 3.0);
		mean = offsetReal.getMean();
		assertEquals(mean, 4, 1e-10);
		
		// logNormal(1, 1, meanInRealSpace=false) + offset=3
		logNormal = new LogNormal(null, new RealScalarParam<>(1, Real.INSTANCE), new RealScalarParam<>(1, PositiveReal.INSTANCE));
		offsetReal = new OffsetReal(logNormal, 3.0);
		mean = offsetReal.getMean();
		assertEquals(mean, 7.4816890703380645, 1e-10);

		
		ScalarDistribution dist = (OffsetReal) fromXML("""
			  <distribution spec='beast.base.spec.inference.distribution.OffsetReal'>
			        <offset spec='beast.base.spec.inference.parameter.RealScalarParam' \
			           domain='Real' value='10'/>
			      <distribution spec='beast.base.spec.inference.distribution.Uniform'>
			        <lower spec='beast.base.spec.inference.parameter.RealScalarParam' \
			           domain='Real' value='0'/>
			        <upper spec='beast.base.spec.inference.parameter.RealScalarParam' \
			          domain='Real' value='1.0'/>
			      </distribution>
		      </distribution>
        """);
		assertEquals(10.5, dist.getMean(), 1e-10);
	}
	

	@Test
    public void testMeanOfGamma() {
		Gamma gamma = new Gamma();
        gamma.initByName("alpha", new RealScalarParam<>(100, PositiveReal.INSTANCE),
                "theta", new RealScalarParam<>(10, PositiveReal.INSTANCE));
		double mean = gamma.getMean();
        assertEquals(mean, 1000, 1e-10);

        gamma = new Gamma();
        gamma.initByName("alpha", new RealScalarParam<>(100, PositiveReal.INSTANCE),
                "theta", new RealScalarParam<>(100, PositiveReal.INSTANCE));
		mean = gamma.getMean();
        assertEquals(mean, 10000, 1e-10);

	}


	@Test
    public void testMeanOfExponential() {
		Exponential exp = new Exponential();
        exp.initByName("mean", new RealScalarParam<>(10, Real.INSTANCE));
		double mean = exp.getMean();
        assertEquals(mean, 10, 1e-10);

        exp = new Exponential();
        exp.initByName("mean", new RealScalarParam<>(1, Real.INSTANCE));
		mean = exp.getMean();
        assertEquals(mean, 1, 1e-10);
	}

	@Test
    public void testMeanOfLogNormal() {
		LogNormal logNormal = new LogNormal();
		logNormal.initByName("M", new RealScalarParam<>(10, Real.INSTANCE),
                "S", new RealScalarParam<>(1, PositiveReal.INSTANCE),
                "meanInRealSpace", true);
		double mean = logNormal.getMean();
        assertEquals(mean, 10, 1e-10);

        logNormal = new LogNormal();
        logNormal.initByName("M", new RealScalarParam<>(1, Real.INSTANCE),
                "S", new RealScalarParam<>(1, PositiveReal.INSTANCE),
                "meanInRealSpace", true);
		mean = logNormal.getMean();
        assertEquals(mean, 1, 1e-10);
	}

	
	@Test
	public void testMeanOfUniform() throws XMLParserException {
        Uniform dist = (Uniform) fromXML("""
                <distribution spec='beast.base.spec.inference.distribution.Uniform'>
                  <lower spec='beast.base.spec.inference.parameter.RealScalarParam' \
                     domain='Real' value='0'/>
                  <upper spec='beast.base.spec.inference.parameter.RealScalarParam' \
                    domain='Real' value='1.0'/>
                </distribution>
                """);
        assertEquals(0.5, dist.getMean(), 1e-10);
        
        dist = (Uniform) fromXML("<input spec='beast.base.spec.inference.distribution.Uniform'/>");
        assertEquals(0.5, dist.getMean(), 1e-10);

        // bounds do not support Inf and -Inf
        Exception exception = assertThrows(
                XMLParserException.class,
                () -> {
                    Uniform uniform = (Uniform) fromXML(""" 
                            <distribution spec='beast.base.spec.inference.distribution.Uniform'>
                              <upper spec='beast.base.spec.inference.parameter.RealScalarParam' \
                                domain='Real' value='Infinity'/>
                            </distribution>
                            """);
                }
        );
        // assert message
        assertTrue(exception.getMessage().contains("Infinity is not finite"));

        exception = assertThrows(
                XMLParserException.class,
                () -> {
                    Uniform uniform = (Uniform) fromXML(""" 
                            <distribution spec='beast.base.spec.inference.distribution.Uniform'>
                              <lower spec='beast.base.spec.inference.parameter.RealScalarParam' \
                                domain='Real' value='-Infinity'/>
                            </distribution>
                            """);
                }
        );
        // assert message
        assertTrue(exception.getMessage().contains("Infinity is not finite"));

        dist = (Uniform) fromXML("""
                <distribution spec='beast.base.spec.inference.distribution.Uniform'>
                  <lower spec='beast.base.spec.inference.parameter.RealScalarParam' \
                     domain='Real' value='-10'/>
                  <upper spec='beast.base.spec.inference.parameter.RealScalarParam' \
                    domain='Real' value='10'/>
                </distribution>
                """);
        assertEquals(0, dist.getMean(), 1e-10);

        dist = new Uniform();
        dist.initByName("lower", new RealScalarParam<>(-1.0, Real.INSTANCE),
                "upper", new RealScalarParam<>(0.0, Real.INSTANCE));
        assertEquals(-0.5, dist.getMean(), 1e-10);
        
	}
	
	BEASTInterface fromXML(String xml) throws XMLParserException {
		return (new XMLParser()).parseBareFragment(xml, true);
	}

}
