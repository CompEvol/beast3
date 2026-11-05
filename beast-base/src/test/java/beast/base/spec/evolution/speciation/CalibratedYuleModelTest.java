package beast.base.spec.evolution.speciation;

import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.CompoundDistribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.evolution.tree.MRCAPrior;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.parameter.RealScalarParam;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A calibrated Yule prior without an explicit "calibrations" input must pick up the calibrations
 * from the spec MRCAPriors next to it in the prior, which is how BEAUti and the example XMLs set it
 * up. It used to look only for the legacy MRCAPrior class, so it found nothing and crashed.
 */
public class CalibratedYuleModelTest {

    private static final String NEWICK = "((A:1.0,B:1.0):2.0,(C:1.5,D:1.5):1.5);";

    private static TaxonSet clade(Tree tree) {
        TaxonSet ab = new TaxonSet();
        ab.initByName("taxon", List.of(new Taxon("A"), new Taxon("B")));
        return ab;
    }

    private static Normal calibrationDistribution() {
        Normal normal = new Normal();
        normal.initByName("mean", new RealScalarParam<>(1.2, Real.INSTANCE),
                "sigma", new RealScalarParam<>(0.5, PositiveReal.INSTANCE));
        return normal;
    }

    @Test
    public void testCalibrationsFoundFromSpecMRCAPrior() {
        Tree tree = new TreeParser(NEWICK, false);

        CalibratedYuleModel yule = new CalibratedYuleModel();
        yule.initByName("tree", tree, "birthRate", new RealScalarParam<>(0.7, PositiveReal.INSTANCE),
                "type", CalibratedYuleModel.Type.OVER_ALL_TOPOS);

        MRCAPrior mrcaPrior = new MRCAPrior();
        mrcaPrior.initByName("tree", tree, "taxonset", clade(tree), "monophyletic", true,
                "distr", calibrationDistribution());

        CompoundDistribution prior = new CompoundDistribution();
        prior.initByName("distribution", List.of(yule, mrcaPrior));
        // the XML parser initialises the Yule prior once its outputs are known; do the same here
        yule.initAndValidate();

        double yuleLogP = yule.calculateLogP();
        double mrcaLogP = mrcaPrior.calculateLogP();
        assertTrue(Double.isFinite(yuleLogP), "calibrated Yule log density must be finite, was " + yuleLogP);

        // the same calibration given explicitly; then the Yule prior includes the calibration density itself
        Tree tree2 = new TreeParser(NEWICK, false);
        CalibrationPoint cal = new CalibrationPoint();
        cal.initByName("taxonset", clade(tree2), "distr", calibrationDistribution());
        CalibratedYuleModel explicit = new CalibratedYuleModel();
        explicit.initByName("tree", tree2, "birthRate", new RealScalarParam<>(0.7, PositiveReal.INSTANCE),
                "type", CalibratedYuleModel.Type.OVER_ALL_TOPOS, "calibrations", List.of(cal));

        assertEquals(explicit.calculateLogP(), yuleLogP + mrcaLogP, 1e-10);
    }
}
