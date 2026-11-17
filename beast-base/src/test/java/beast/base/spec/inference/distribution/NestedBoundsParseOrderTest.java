package beast.base.spec.inference.distribution;

import beast.base.parser.XMLParser;
import beast.base.parser.XMLParserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #286: the XML parser initialises a parameter declared in the state before the
 * lower/upper parameters nested in its prior, which still read 0 at that point. The
 * parameter must not be validated against those bounds then; the prior validates it
 * once its own inputs are initialised.
 */
public class NestedBoundsParseOrderTest {

    @TempDir
    Path dir;

    private static final String INT = "beast.base.spec.inference.parameter.IntScalarParam";
    private static final String REAL = "beast.base.spec.inference.parameter.RealScalarParam";
    private static final String DIST = "beast.base.spec.inference.distribution.";

    private static String intParam(String name, int value) {
        return "<" + name + " spec='" + INT + "' domain='Int' value='" + value + "' estimate='false'/>";
    }

    private static String model(String stateNode, String prior, String operator) {
        return "<beast version='2.8' namespace='beast.base.inference:beast.base.inference.util'>\n"
                + "<run spec='MCMC' id='mcmc' chainLength='1000'>\n"
                + "  <state id='state'>" + stateNode + "</state>\n"
                + "  <distribution spec='CompoundDistribution' id='posterior'>" + prior + "</distribution>\n"
                + "  " + operator + "\n"
                + "  <logger logEvery='1000'><log idref='posterior'/></logger>\n"
                + "</run>\n</beast>\n";
    }

    private Object parse(String xml) throws Exception {
        File file = dir.resolve("model.xml").toFile();
        Files.writeString(file.toPath(), xml);
        return new XMLParser().parseFile(file);
    }

    private static final String K = "<stateNode id='k' spec='" + INT + "' domain='Int' value='5'/>";
    private static final String K_WALK = "<operator spec='beast.base.spec.inference.operator.IntRandomWalkOperator' scalar='@k' windowSize='1' weight='1'/>";

    @Test
    public void testIntUniformWithNestedBounds() throws Exception {
        String prior = "<distribution id='kPrior' spec='" + DIST + "IntUniform' param='@k'>"
                + intParam("lower", 0) + intParam("upper", 10) + "</distribution>";
        assertNotNull(parse(model(K, prior, K_WALK)));
    }

    @Test
    public void testBoundedIntWithNestedBounds() throws Exception {
        String prior = "<distribution id='kPrior' spec='" + DIST + "TruncatedInt' param='@k'>"
                + intParam("lower", 0) + intParam("upper", 10)
                + "<distribution spec='" + DIST + "Poisson'>"
                + "<lambda spec='" + REAL + "' domain='NonNegativeReal' value='2.0' estimate='false'/>"
                + "</distribution></distribution>";
        assertNotNull(parse(model(K, prior, K_WALK)));
    }

    @Test
    public void testIIDOfIntUniformWithNestedBounds() throws Exception {
        String v = "<stateNode id='v' spec='beast.base.spec.inference.parameter.IntVectorParam' domain='Int' value='5 6 7'/>";
        String prior = "<distribution id='vPrior' spec='" + DIST + "IID' param='@v'>"
                + "<distr spec='" + DIST + "IntUniform'>" + intParam("lower", 0) + intParam("upper", 10) + "</distr>"
                + "</distribution>";
        String op = "<operator spec='beast.base.spec.inference.operator.IntRandomWalkOperator' parameter='@v' windowSize='1' weight='1'/>";
        assertNotNull(parse(model(v, prior, op)));
    }

    @Test
    public void testRealUniformWithNestedBounds() throws Exception {
        String y = "<stateNode id='y' spec='" + REAL + "' domain='PositiveReal' value='5.0'/>";
        String prior = "<distribution id='yPrior' spec='" + DIST + "Uniform' param='@y'>"
                + "<lower spec='" + REAL + "' domain='Real' value='0.0' estimate='false'/>"
                + "<upper spec='" + REAL + "' domain='Real' value='10.0' estimate='false'/>"
                + "</distribution>";
        String op = "<operator spec='beast.base.spec.inference.operator.ScaleOperator' parameter='@y' scaleFactor='0.5' weight='1'/>";
        assertNotNull(parse(model(y, prior, op)));
    }

    /**
     * A value outside the prior's bounds is still rejected, now by the prior once its
     * bounds are initialised, rather than by the parameter.
     */
    @Test
    public void testValueOutsideNestedBoundsIsStillRejected() {
        String k = "<stateNode id='k' spec='" + INT + "' domain='Int' value='15'/>";
        String prior = "<distribution id='kPrior' spec='" + DIST + "IntUniform' param='@k'>"
                + intParam("lower", 0) + intParam("upper", 10) + "</distribution>";
        XMLParserException e = assertThrows(XMLParserException.class, () -> parse(model(k, prior, K_WALK)));
        assertTrue(e.getMessage().contains("not valid"), e.getMessage());
        assertTrue(e.getMessage().contains("[0,10]"), e.getMessage());
    }

    //*** outside XML loading, priors bound their parameter as before (review of #288) ***//

    /**
     * A prior built in code and never initialised, as BEAUti editors or package code may
     * build one, bounds its parameter as on master: the bounds are skipped only while the
     * XML parser initialises its objects.
     */
    @Test
    public void testPriorBuiltInCodeWithoutInit() {
        beast.base.spec.inference.parameter.IntScalarParam<beast.base.spec.domain.Int> x =
                new beast.base.spec.inference.parameter.IntScalarParam<>(3, beast.base.spec.domain.Int.INSTANCE);
        x.setID("x");
        IntUniform uniform = new IntUniform();
        uniform.setInputValue("lower", new beast.base.spec.inference.parameter.IntScalarParam<>(0, beast.base.spec.domain.Int.INSTANCE));
        uniform.setInputValue("upper", new beast.base.spec.inference.parameter.IntScalarParam<>(5, beast.base.spec.domain.Int.INSTANCE));
        uniform.setInputValue("param", x);
        assertFalse(uniform.isAwaitingInit());
        assertEquals(0, x.getLower());
        assertEquals(5, x.getUpper());
        assertFalse(x.isValid(7));
    }

    /**
     * A distribution whose initAndValidate() does not call super's never clears its own
     * mark, so the parser clears it once it has initialised everything, and parameters
     * use its bounds after loading as before. MarkovChainDistribution is such a class.
     */
    @Test
    public void testParserClearsMarkOfDistributionNotCallingSuper() throws Exception {
        String v = "<stateNode id='v' spec='beast.base.spec.inference.parameter.RealVectorParam' domain='PositiveReal' value='1.0 2.0 3.0'/>";
        String prior = "<distribution id='vPrior' spec='" + DIST + "MarkovChainDistribution' param='@v' shape='1.0' jeffreys='true'/>";
        String op = "<operator spec='beast.base.spec.inference.operator.ScaleOperator' parameter='@v' scaleFactor='0.5' weight='1'/>";
        beast.base.inference.MCMC mcmc = (beast.base.inference.MCMC) parse(model(v, prior, op));
        int checked = 0;
        for (beast.base.inference.StateNode n : mcmc.startStateInput.get().stateNodeInput.get()) {
            for (beast.base.core.BEASTInterface o : n.getOutputs()) {
                if (o instanceof TensorDistribution<?, ?> d) {
                    assertFalse(d.isAwaitingInit(), "mark left on " + d.getClass().getSimpleName());
                    checked++;
                }
            }
        }
        assertTrue(checked > 0, "no distribution found on the parameter");
    }

    /** after a normal load, the nested bounds apply and no mark is left */
    @Test
    public void testBoundsApplyAfterLoading() throws Exception {
        String prior = "<distribution id='kPrior' spec='" + DIST + "IntUniform' param='@k'>"
                + intParam("lower", 0) + intParam("upper", 10) + "</distribution>";
        beast.base.inference.MCMC mcmc = (beast.base.inference.MCMC) parse(model(K, prior, K_WALK));
        beast.base.spec.type.IntScalar<?> k = null;
        for (beast.base.inference.StateNode n : mcmc.startStateInput.get().stateNodeInput.get()) {
            if ("k".equals(n.getID())) {
                k = (beast.base.spec.type.IntScalar<?>) n;
            }
        }
        assertNotNull(k);
        assertEquals(0, k.getLower());
        assertEquals(10, k.getUpper());
    }

    //*** an IntUniformOperator listed before its prior (review of #288) ***//

    private static final String K_UNIFORM_OP =
            "<operator spec='beast.base.spec.inference.operator.uniform.IntUniformOperator' parameter='@k' weight='1'/>";

    /** as model(), with the operator before the distribution, so it is initialised first */
    private static String modelOperatorFirst(String stateNode, String prior, String operator) {
        return "<beast version='2.8' namespace='beast.base.inference:beast.base.inference.util'>\n"
                + "<run spec='MCMC' id='mcmc' chainLength='2000'>\n"
                + "  <state id='state'>" + stateNode + "</state>\n"
                + "  " + operator + "\n"
                + "  <distribution spec='CompoundDistribution' id='posterior'>" + prior + "</distribution>\n"
                + "  <logger logEvery='1000'><log idref='posterior'/></logger>\n"
                + "</run>\n</beast>\n";
    }

    private static beast.base.spec.type.IntScalar<?> stateNode(beast.base.inference.MCMC mcmc, String id) {
        for (beast.base.inference.StateNode n : mcmc.startStateInput.get().stateNodeInput.get()) {
            if (id.equals(n.getID())) {
                return (beast.base.spec.type.IntScalar<?>) n;
            }
        }
        return null;
    }

    /**
     * IntUniformOperator used to cache its bounds when initialised. Listed before its prior,
     * it was initialised while the prior was still marked, cached the domain's bounds
     * [MIN_VALUE, MAX_VALUE], and its first proposal threw "n must be positive" (the range
     * overflowed). It now reads the bounds in proposal().
     */
    @Test
    public void testIntUniformOperatorBeforePrior() throws Exception {
        String prior = "<distribution id='kPrior' spec='" + DIST + "IntUniform' param='@k'>"
                + intParam("lower", 0) + intParam("upper", 10) + "</distribution>";
        beast.base.inference.MCMC mcmc = (beast.base.inference.MCMC) parse(modelOperatorFirst(K, prior, K_UNIFORM_OP));
        assertDoesNotThrow(mcmc::run);
        int k = stateNode(mcmc, "k").get();
        assertTrue(k >= 0 && k <= 10, "k = " + k);
    }

    /** the same with Bounded(Poisson) and the operator first */
    @Test
    public void testIntUniformOperatorBeforeBoundedPrior() throws Exception {
        String prior = "<distribution id='kPrior' spec='" + DIST + "TruncatedInt' param='@k'>"
                + intParam("lower", 0) + intParam("upper", 10)
                + "<distribution spec='" + DIST + "Poisson'>"
                + "<lambda spec='" + REAL + "' domain='NonNegativeReal' value='2.0' estimate='false'/>"
                + "</distribution></distribution>";
        beast.base.inference.MCMC mcmc = (beast.base.inference.MCMC) parse(modelOperatorFirst(K, prior, K_UNIFORM_OP));
        assertDoesNotThrow(mcmc::run);
        int k = stateNode(mcmc, "k").get();
        assertTrue(k >= 0 && k <= 10, "k = " + k);
    }

    //*** JSON (review of #288) ***//

    /**
     * JSONParser initialises objects in the same order as XMLParser, so it needs the same
     * marks: the model with an IntUniform prior with nested bounds, written as JSON, loads.
     */
    @Test
    public void testJSONWithNestedBounds() throws Exception {
        String prior = "<distribution id='kPrior' spec='" + DIST + "IntUniform' param='@k'>"
                + intParam("lower", 0) + intParam("upper", 10) + "</distribution>";
        beast.base.inference.MCMC mcmc = (beast.base.inference.MCMC) parse(model(K, prior, K_WALK));
        String json = new beast.base.parser.JSONProducer().toJSON(mcmc);
        java.io.File file = dir.resolve("model.json").toFile();
        Files.writeString(file.toPath(), json);
        beast.base.inference.MCMC fromJSON = (beast.base.inference.MCMC) new beast.base.parser.JSONParser().parseFile(file);
        beast.base.spec.type.IntScalar<?> k = stateNode(fromJSON, "k");
        assertNotNull(k);
        assertEquals(0, k.getLower());
        assertEquals(10, k.getUpper());
    }
}
