package beast.base.spec.evolution.speciation;

import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.parameter.RealParameter;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The spec SpeciesTreePrior gives the same log density as the legacy one:
 * Gamma(shape 2, scale psi) on the top (or constant) population sizes, and
 * Gamma(shape 4, scale psi) on the tip bottom population sizes (#326 review).
 */
public class SpeciesTreePriorTest {

    static final String NEWICK = "((A:1.0,B:1.0):1.0,C:2.0);";
    static final double[] BOTTOM = {0.1, 0.5, 1.0, 2.0, 0.3};
    static final double[] TOP = {0.2, 0.7, 1.5, 0.4, 3.0};

    static TaxonSet taxonSet() {
        return new TaxonSet(List.of(new Taxon("A"), new Taxon("B"), new Taxon("C")));
    }

    static double legacyLogP(String popFunction, double psi) {
        Tree tree = new TreeParser(NEWICK, false);
        beast.base.evolution.speciation.SpeciesTreePrior prior =
                new beast.base.evolution.speciation.SpeciesTreePrior();
        prior.initByName("tree", tree, "taxonset", taxonSet(), "popFunction", popFunction,
                "gammaParameter", new RealParameter(new Double[] {psi}),
                "bottomPopSize", new RealParameter(box(BOTTOM)),
                "topPopSize", new RealParameter(box(TOP)));
        return prior.calculateLogP();
    }

    static double specLogP(String popFunction, double psi) {
        Tree tree = new TreeParser(NEWICK, false);
        SpeciesTreePrior prior = new SpeciesTreePrior();
        prior.initByName("tree", tree, "taxonset", taxonSet(), "popFunction", popFunction,
                "gammaParameter", new RealScalarParam<>(psi, PositiveReal.INSTANCE),
                "bottomPopSize", new RealVectorParam<>(BOTTOM.clone(), PositiveReal.INSTANCE),
                "topPopSize", new RealVectorParam<>(TOP.clone(), PositiveReal.INSTANCE));
        return prior.calculateLogP();
    }

    static Double[] box(double[] x) {
        Double[] y = new Double[x.length];
        for (int i = 0; i < x.length; i++) y[i] = x[i];
        return y;
    }

    @Test
    public void testSameAsLegacy() {
        for (String popFunction : new String[] {"constant", "linear", "linear_with_constant_root"}) {
            for (double psi : new double[] {0.5, 1.0, 3.0}) {
                assertEquals(legacyLogP(popFunction, psi), specLogP(popFunction, psi), 1e-10,
                        popFunction + ", psi = " + psi);
            }
        }
    }

    /** constant: logP is the sum of Gamma(2, scale psi) log densities over the population sizes */
    @Test
    public void testConstantByHand() {
        double psi = 0.5;
        double expected = 0;
        for (double x : BOTTOM) {
            // log of x^(2-1) e^(-x/psi) / (Gamma(2) psi^2)
            expected += Math.log(x) - x / psi - 2 * Math.log(psi);
        }
        assertEquals(expected, specLogP("constant", psi), 1e-10);
    }
}
