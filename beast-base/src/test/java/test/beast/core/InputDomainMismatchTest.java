package test.beast.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import beast.base.spec.evolution.substitutionmodel.ComplexSubstitutionModel;
import beast.base.spec.inference.parameter.CompoundRealScalarParam;
import beast.base.spec.inference.parameter.IntSimplexParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.VectorElement;
import beast.base.spec.evolution.Sum;
import beast.base.spec.inference.util.AsRealScalar;

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

    /** a value whose domain is fixed gets advice that can be followed: wrap it */
    @Test
    void valueWithFixedDomainIsToldToWrap() {
        Sum sum = new Sum();
        sum.setID("total");
        sum.initByName("arg", new RealScalarParam<>(0.5, PositiveReal.INSTANCE));
        Gamma gamma = new Gamma();
        gamma.initByName("alpha", sum, "theta", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        String message = gamma.alphaInput.domainMismatch(gamma);
        assertNotNull(message);
        assertTrue(message.contains("AsRealScalar") && message.contains("domain=\"PositiveReal\""), message);
        assertFalse(message.contains("give 'total' domain="), message);

        // following the advice removes the mismatch
        AsRealScalar<PositiveReal> wrapped = new AsRealScalar<>();
        wrapped.initByName("arg", sum, "domain", PositiveReal.INSTANCE);
        Gamma fixed = new Gamma();
        fixed.initByName("alpha", wrapped, "theta", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        assertNull(fixed.alphaInput.domainMismatch(fixed));
    }

    /** a VectorElement's domain is known before it is initialised, as the parser validates first */
    @Test
    void vectorElementDomainIsSeenBeforeInitialisation() {
        RealVectorParam<Real> v = new RealVectorParam<>(new double[] {0.5, 1.0}, Real.INSTANCE);
        VectorElement<Real> element = new VectorElement<>();
        element.vectorInput.setValue(v, element);
        element.indexInput.setValue(0, element);
        assertEquals(Real.INSTANCE, element.getDomain());
        Gamma gamma = new Gamma();
        gamma.alphaInput.setValue(element, gamma);
        assertNotNull(gamma.alphaInput.domainMismatch(gamma), "VectorElement of a Real vector fits PositiveReal");
    }

    /**
     * A compound parameter's domain is that of the parameters inside it, so the advice is to
     * give them the domain; its own domain input would change nothing (Walter's review of #303)
     */
    @Test
    void compoundParameterIsToldToChangeItsParameters() {
        ComplexSubstitutionModel csm = new ComplexSubstitutionModel();
        csm.setID("csm");
        csm.ratesInput.setValue(compound(Real.INSTANCE), csm);
        String message = csm.ratesInput.domainMismatch(csm);
        assertNotNull(message);
        assertTrue(message.contains("parameters inside it") && message.contains("domain=\"PositiveReal\""), message);
        assertFalse(message.contains("give 'rates' domain="), message);

        // following the advice removes the mismatch
        csm.ratesInput.setValue(compound(PositiveReal.INSTANCE), csm);
        assertNull(csm.ratesInput.domainMismatch(csm));
    }

    private static CompoundRealScalarParam<?> compound(Real domain) {
        CompoundRealScalarParam<Real> rates = new CompoundRealScalarParam<>();
        rates.setID("rates");
        rates.parameterListInput.get().add(new RealScalarParam<>(0.5, domain));
        rates.parameterListInput.get().add(new RealScalarParam<>(1.0, domain));
        return rates;
    }

    /** an element of a vector is told to change the vector, which is what fixes it */
    @Test
    void vectorElementIsToldToChangeItsVector() {
        RealVectorParam<Real> v = new RealVectorParam<>(new double[] {0.5, 1.0}, Real.INSTANCE);
        v.setID("rateVector");
        VectorElement<Real> element = new VectorElement<>();
        element.vectorInput.setValue(v, element);
        element.indexInput.setValue(0, element);
        Gamma gamma = new Gamma();
        gamma.alphaInput.setValue(element, gamma);
        String message = gamma.alphaInput.domainMismatch(gamma);
        assertNotNull(message);
        assertTrue(message.contains("give 'rateVector' domain=\"PositiveReal\""), message);
    }

    /** a domain set after getDomain() was called, before init, is seen */
    @Test
    void domainSetLaterBeforeInitIsSeen() {
        RealScalarParam<Real> p = new RealScalarParam<>();
        p.domainTypeInput.setValue(Real.INSTANCE, p);
        assertEquals(Real.INSTANCE, p.getDomain());
        p.domainTypeInput.setValue(PositiveReal.INSTANCE, p);
        assertEquals(PositiveReal.INSTANCE, p.getDomain());
    }

    /** two objects without IDs and the same mismatch are both warned about */
    @Test
    void sameMismatchOnTwoObjectsIsWarnedForEach() {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        java.io.PrintStream old = beast.base.core.Log.warning;
        beast.base.core.Log.warning = new java.io.PrintStream(out, true);
        try {
            for (int i = 0; i < 2; i++) {
                Gamma gamma = new Gamma();
                gamma.initByName("alpha", new RealScalarParam<>(0.5, Real.INSTANCE),
                        "theta", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
            }
        } finally {
            beast.base.core.Log.warning = old;
        }
        String log = out.toString();
        assertEquals(2, log.split("is declared PositiveReal", -1).length - 1, log);
    }

    /** a mismatch that is fixed and comes back is warned about again */
    @Test
    void mismatchThatComesBackIsWarnedAgain() {
        Gamma gamma = gammaWithAlpha(new RealScalarParam<>(0.5, Real.INSTANCE));
        String message = gamma.alphaInput.domainMismatch(gamma);
        assertTrue(DomainMismatchWarnings.warned(message));
        Object real = gamma.alphaInput.get();
        gamma.alphaInput.setValue(new RealScalarParam<>(0.5, PositiveReal.INSTANCE), gamma);
        gamma.validateInputs();
        assertFalse(DomainMismatchWarnings.warned(message), "fixed mismatch still recorded");
        gamma.alphaInput.setValue(real, gamma);
        gamma.validateInputs();
        assertTrue(DomainMismatchWarnings.warned(message), "returning mismatch not logged");
    }

    @Test
    void nullOwnerIsAllowed() {
        Gamma gamma = gammaWithAlpha(new RealScalarParam<>(0.5, Real.INSTANCE));
        String message = gamma.alphaInput.domainMismatch(null);
        assertNotNull(message);
        assertTrue(message.startsWith("Input 'alpha' is declared PositiveReal"), message);
    }

    /** before init, an invalid value gives the intended error, not a NullPointerException */
    @Test
    void intSimplexInvalidValueBeforeInitGivesIllegalArgument() {
        IntSimplexParam<NonNegativeInt> p = new IntSimplexParam<>();
        assertThrows(IllegalArgumentException.class, () -> p.setLower(-1));
    }
}
