package beast.base.spec.evolution.sitemodel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import beast.base.core.Log;
import beast.base.inference.CalculationNode;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.evolution.substitutionmodel.JukesCantor;
import beast.base.spec.inference.distribution.Uniform;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;

/**
 * SiteModel warns when the lower bound of the gamma shape is below 1e-3, where the quantile
 * calculator fails. The warning must not depend on the class of the shape (issue #258).
 */
public class SiteModelShapeWarningTest {

    private static final String WARNING = "The quantile calculator in SiteModel fails";

    /** a shape that is a RealScalar but not a RealScalarParam, such as a value computed from other parameters */
    public static class ComputedShape extends CalculationNode implements RealScalar<PositiveReal> {
        @Override
        public void initAndValidate() {
        }

        @Override
        public double get() {
            return 0.5;
        }

        @Override
        public PositiveReal getDomain() {
            return PositiveReal.INSTANCE;
        }
    }

    private PrintStream oldWarning;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void captureWarnings() {
        oldWarning = Log.warning;
        captured = new ByteArrayOutputStream();
        Log.warning = new PrintStream(captured, true);
    }

    @AfterEach
    void restoreWarnings() {
        Log.warning = oldWarning;
    }

    private String warningsFor(RealScalar<PositiveReal> shape) {
        SiteModel siteModel = new SiteModel();
        siteModel.initByName("gammaCategoryCount", 4, "shape", shape, "substModel", new JukesCantor());
        return captured.toString();
    }

    @Test
    public void warnsForParameterWithLowerBoundZero() {
        assertTrue(warningsFor(new RealScalarParam<>(0.5, PositiveReal.INSTANCE)).contains(WARNING));
    }

    @Test
    public void warnsForShapeThatIsNotAParameter() {
        ComputedShape shape = new ComputedShape();
        shape.initAndValidate();
        assertTrue(warningsFor(shape).contains(WARNING),
                "no warning for a shape with lower bound 0 that is not a RealScalarParam");
    }

    @Test
    public void noWarningWhenThePriorKeepsTheShapeAwayFromZero() {
        RealScalarParam<PositiveReal> shape = new RealScalarParam<>(0.5, PositiveReal.INSTANCE);
        shape.setID("shape"); // a prior finds its parameter by ID
        Uniform prior = new Uniform();
        prior.initByName("param", shape,
                "lower", new RealScalarParam<>(0.01, PositiveReal.INSTANCE),
                "upper", new RealScalarParam<>(100.0, PositiveReal.INSTANCE));
        assertFalse(warningsFor(shape).contains(WARNING), captured.toString());
    }
}
