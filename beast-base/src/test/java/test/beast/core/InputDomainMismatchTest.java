package test.beast.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import beast.base.core.BEASTInterface;
import beast.base.core.DomainMismatchWarnings;
import beast.base.parser.XMLParser;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.Gamma;
import beast.base.spec.inference.distribution.Poisson;
import beast.base.spec.inference.parameter.IntSimplexParam;
import beast.base.spec.inference.parameter.RealScalarParam;

/**
 * A tensor input given a value whose domain does not fit the declared one, e.g. a Real
 * parameter for Gamma's alpha (declared PositiveReal), is deprecated (#226): the model is
 * accepted, with a warning that it will be an error in a future version. A value whose
 * domain is the declared one or a sub-domain of it fits.
 */
public class InputDomainMismatchTest {

    @BeforeEach
    void forgetWarnings() {
        DomainMismatchWarnings.reset();
    }

    private static Gamma gammaWithAlpha(RealScalarParam<?> alpha) {
        alpha.setID("alpha");
        Gamma gamma = new Gamma();
        gamma.setID("xPrior");
        gamma.initByName("alpha", alpha, "theta", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        return gamma;
    }

    @Test
    void realParameterForPositiveRealInputIsWarnedAbout() {
        Gamma gamma = gammaWithAlpha(new RealScalarParam<>(0.5, Real.INSTANCE));

        String message = gamma.alphaInput.domainMismatch(gamma);
        assertNotNull(message, "no mismatch for a Real alpha");
        assertTrue(message.contains("'alpha'") && message.contains("Gamma 'xPrior'"), message);
        assertTrue(message.contains("declared PositiveReal") && message.contains("domain Real"), message);
        assertTrue(message.contains("deprecated") && message.contains("error in a future version"), message);
        assertTrue(message.contains("domain=\"PositiveReal\""), message);
        // logged when the inputs were validated, and the model is still accepted
        assertTrue(DomainMismatchWarnings.warned(message), "not logged: " + message);
        assertEquals(0.5, gamma.alphaInput.get().get(), 0.0);
    }

    @Test
    void sameDomainFits() {
        Gamma gamma = gammaWithAlpha(new RealScalarParam<>(0.5, PositiveReal.INSTANCE));
        assertNull(gamma.alphaInput.domainMismatch(gamma));
    }

    /** e.g. a PositiveReal rate for Poisson's lambda, declared NonNegativeReal */
    @Test
    void subDomainFits() {
        Poisson poisson = new Poisson(null, new RealScalarParam<>(2.0, PositiveReal.INSTANCE));
        assertNull(poisson.lambdaInput.domainMismatch(poisson));
        Poisson nonNegative = new Poisson(null, new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE));
        assertNull(nonNegative.lambdaInput.domainMismatch(nonNegative));
    }

    /** the example from #226: loads, with the warning */
    @Test
    void xmlWithRealParameterLoadsWithWarning() throws Exception {
        String xml = "<beast version='2.8' namespace='beast.base.spec.inference.distribution:beast.base.spec.inference.parameter'>"
                + "<distribution id='xPrior' spec='Gamma'>"
                + "  <param id='x' spec='RealScalarParam' domain='PositiveReal' value='1.0'/>"
                + "  <alpha id='alphaParam' spec='RealScalarParam' domain='Real' value='0.5'/>"
                + "  <theta spec='RealScalarParam' domain='PositiveReal' value='1.0'/>"
                + "</distribution>"
                + "</beast>";
        BEASTInterface o = new XMLParser().parseBareFragment(xml, true);
        Gamma gamma = (Gamma) o;
        String message = gamma.alphaInput.domainMismatch(gamma);
        assertNotNull(message);
        assertTrue(message.contains("'alphaParam'"), message);
        assertTrue(DomainMismatchWarnings.warned(message), "not logged while parsing: " + message);
    }

    /**
     * An IntSimplexParam's domain set in XML, e.g. PositiveInt for BSP group sizes, is seen
     * before it is initialised, when the parser checks the inputs it is given to.
     */
    @Test
    void intSimplexDomainIsSeenBeforeInitialisation() {
        IntSimplexParam<PositiveInt> groupSizes = new IntSimplexParam<>();
        assertEquals(NonNegativeInt.INSTANCE, groupSizes.getDomain());
        groupSizes.domainTypeInput.setValue(PositiveInt.INSTANCE, groupSizes);
        assertEquals(PositiveInt.INSTANCE, groupSizes.getDomain());
    }
}
