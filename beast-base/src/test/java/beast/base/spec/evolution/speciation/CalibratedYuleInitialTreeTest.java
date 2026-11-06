package beast.base.spec.evolution.speciation;

import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.Uniform;
import beast.base.spec.inference.parameter.RealScalarParam;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The spec CalibratedYuleInitialTree takes spec CalibrationPoints. It used to pass them to a legacy
 * CalibratedYuleModel, which rejected them with a type mismatch, so it could not be used at all.
 */
public class CalibratedYuleInitialTreeTest {

    @Test
    public void testStartingTreeRespectsSpecCalibration() {
        List<Taxon> taxa = new ArrayList<>();
        for (String name : new String[]{"a", "b", "c", "d", "e"}) {
            taxa.add(new Taxon(name));
        }
        TaxonSet all = new TaxonSet();
        all.initByName("taxon", taxa);

        TaxonSet ab = new TaxonSet();
        ab.initByName("taxon", List.of(taxa.get(0), taxa.get(1)));
        Uniform uniform = new Uniform();
        uniform.initByName("lower", new RealScalarParam<>(4.0, Real.INSTANCE),
                "upper", new RealScalarParam<>(7.0, Real.INSTANCE));
        CalibrationPoint cal = new CalibrationPoint();
        cal.initByName("taxonset", ab, "distr", uniform);

        Tree tree = new Tree();
        tree.initByName("taxonset", all);

        CalibratedYuleInitialTree init = new CalibratedYuleInitialTree();
        init.initByName("taxonset", all, "calibrations", List.of(cal), "initial", tree);

        assertEquals(5, tree.getLeafNodeCount());
        Node mrca = mrca(tree.getRoot(), Set.of("a", "b"));
        assertEquals(2, mrca.getLeafNodeCount(), "{a, b} must be a clade in the starting tree");
        double h = mrca.getHeight();
        assertTrue(h >= 4.0 && h <= 7.0, "calibrated clade height " + h + " must be inside [4, 7]");
    }

    private static Node mrca(Node node, Set<String> names) {
        for (Node child : node.getChildren()) {
            if (leafNames(child).containsAll(names)) {
                return mrca(child, names);
            }
        }
        return node;
    }

    private static Set<String> leafNames(Node node) {
        Set<String> names = new java.util.HashSet<>();
        for (Node leaf : node.getAllLeafNodes()) {
            names.add(leaf.getID());
        }
        return names;
    }
}
