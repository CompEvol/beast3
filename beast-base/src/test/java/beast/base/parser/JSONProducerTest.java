package beast.base.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import beast.base.core.BEASTInterface;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.SimplexParam;
import beast.base.spec.type.IntVector;
import beast.base.spec.type.RealScalar;

/**
 * JSONProducer must write a parameter's domain as its short class name
 * ({@code domain: "PositiveReal"}), as XMLProducer does, so the JSON can be read back.
 *
 * <p>Domains are singletons shared by every parameter with that domain. Before the fix,
 * JSONProducer wrote the first use as an object without an id
 * ({@code domain: {spec: "beast.base.spec.domain.PositiveReal"}}) and every later use
 * as {@code domain: "@null"}; JSONParser then failed with
 * {@code NoSuchMethodException: beast.base.spec.domain.PositiveReal.newInstance()}.
 */
public class JSONProducerTest {

    /** Normal(mean, sigma) prior on kappa: kappa and sigma share the PositiveReal singleton. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Normal model() {
        // raw types, as in RealScalarParamTest: Normal's constructor takes RealScalar<Real>
        RealScalarParam kappa = new RealScalarParam(2.0, PositiveReal.INSTANCE);
        kappa.setID("kappa");
        RealScalarParam mean = new RealScalarParam(0.0, Real.INSTANCE);
        mean.setID("mean");
        RealScalarParam sigma = new RealScalarParam(1.0, PositiveReal.INSTANCE);
        sigma.setID("sigma");
        Normal normal = new Normal(kappa, mean, sigma);
        normal.setID("normal");
        return normal;
    }

    @Test
    void domainIsWrittenAsShortName() {
        String json = new JSONProducer().toJSON(model());

        assertTrue(json.contains("domain: \"PositiveReal\""), json);
        assertTrue(json.contains("domain: \"Real\""), json);
        assertFalse(json.contains("beast.base.spec.domain."), "domain written as an object:\n" + json);
        assertFalse(json.contains("\"@null\""), "dangling reference:\n" + json);
    }

    @Test
    void sharedDomainIsWrittenForEveryParameter() {
        // kappa and sigma both have PositiveReal: the second use used to become "@null".
        String json = new JSONProducer().toJSON(model());

        int count = json.split("domain: \"PositiveReal\"", -1).length - 1;
        assertEquals(2, count, json);
    }

    @Test
    void roundTripKeepsDomains() throws Exception {
        String json = new JSONProducer().toJSON(model());

        List<Object> objects = new JSONParser().parseFragment(json, true);
        Normal normal = (Normal) objects.get(objects.size() - 1);

        RealScalar<?> kappa = (RealScalar<?>) ((BEASTInterface) normal).getInput("param").get();
        RealScalar<?> mean = (RealScalar<?>) ((BEASTInterface) normal).getInput("mean").get();
        RealScalar<?> sigma = (RealScalar<?>) ((BEASTInterface) normal).getInput("sigma").get();
        assertSame(PositiveReal.INSTANCE, kappa.getDomain());
        assertSame(Real.INSTANCE, mean.getDomain());
        assertSame(PositiveReal.INSTANCE, sigma.getDomain());
        assertEquals(2.0, kappa.get(), 0.0);
    }

    @Test
    void domainFixedByTheClassIsNotWritten() throws Exception {
        // SimplexParam sets its own UnitInterval domain; writing it again made JSONParser
        // fail with "Multiple entries for non-list input domain".
        SimplexParam freqs = new SimplexParam(new double[] {0.1, 0.2, 0.3, 0.4});
        freqs.setID("freqs");

        String json = new JSONProducer().toJSON(freqs);
        assertFalse(json.contains("domain:"), json);

        List<Object> objects = new JSONParser().parseFragment(json, true);
        SimplexParam parsed = (SimplexParam) objects.get(objects.size() - 1);
        assertSame(UnitInterval.INSTANCE, parsed.getDomain());
        assertEquals(0.4, parsed.get(3), 1e-12);
    }

    @Test
    void roundTripKeepsIntegerDomain() throws Exception {
        IntVectorParam<NonNegativeInt> categories =
                new IntVectorParam<>(new int[] {0, 1, 2}, NonNegativeInt.INSTANCE);
        categories.setID("categories");

        String json = new JSONProducer().toJSON(categories);
        assertTrue(json.contains("domain: \"NonNegativeInt\""), json);

        List<Object> objects = new JSONParser().parseFragment(json, true);
        IntVector<?> parsed = (IntVector<?>) objects.get(objects.size() - 1);
        assertSame(NonNegativeInt.INSTANCE, parsed.getDomain());
        assertEquals(3, parsed.size());
    }
}
