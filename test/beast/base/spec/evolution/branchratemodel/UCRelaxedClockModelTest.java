package beast.base.spec.evolution.branchratemodel;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.tree.Tree;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.LogNormal;
import beast.base.spec.inference.distribution.Poisson;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.IntVector;
import beast.base.spec.type.RealScalar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.beast.BEASTTestCase;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UCRelaxedClockModelTest extends BEASTTestCase {

    UCRelaxedClockModel ucRelaxedClockModel;
    Tree tree;
    LogNormal logNormal;

    @BeforeEach
    void setUp() throws Exception {
        ucRelaxedClockModel = new UCRelaxedClockModel();

        RealScalarParam<Real> m = new RealScalarParam<>(1.0, Real.INSTANCE);
        RealScalarParam<PositiveReal> s = new RealScalarParam<>(0.3333333333333333, PositiveReal.INSTANCE);
        logNormal = new LogNormal(null, m, s, true);
        logNormal.setID("lognormal");


        Sequence a = new Sequence("A", "A");
        Sequence b = new Sequence("B", "A");
        Sequence c = new Sequence("C", "A");
        Alignment data = new Alignment();
        data.initByName("sequence", a, "sequence", b, "sequence", c, "dataType", "nucleotide");
        tree = getTree(data, "((A:1.0,B:1.0):1.0,C:2.0);");

    }

    @Test
    void testDistr() {

        RealScalarParam<NonNegativeReal> lambda = new RealScalarParam<>(1.0, NonNegativeReal.INSTANCE);
        // ScalarDistribution<IntScalar<NonNegativeInt>, Integer>
        Poisson poisson = new Poisson(null, lambda);

        int[] c = new int[10];
        Arrays.fill(c, 1);
        IntVectorParam<NonNegativeInt> rateCategories = new IntVectorParam<>(c, NonNegativeInt.INSTANCE);

        ucRelaxedClockModel.initByName("distr", poisson, "rateCategories", rateCategories, "tree", tree);

        //TODO how this is passed ?
        ScalarDistribution<RealScalar<Real>, Double> distr = ucRelaxedClockModel.getDistribution();
        assertTrue(distr != null &&
                distr instanceof ScalarDistribution<RealScalar<Real>, Double>);

        IntVector<NonNegativeInt> cate = ucRelaxedClockModel.getCategories();
        assertEquals(4, cate.size());
    }


    @Test
    void testCategoriesModeInit() {
        // 2n−2
        int[] c = new int[4];
        Arrays.fill(c, 1);
        IntVectorParam<NonNegativeInt> rateCategories = new IntVectorParam<>(c, NonNegativeInt.INSTANCE);

        ucRelaxedClockModel.initByName("distr", logNormal, "rateCategories", rateCategories, "tree", tree);

        IntVector<NonNegativeInt> cate = ucRelaxedClockModel.getCategories();
        assertEquals(c.length, cate.size());

        assertEquals(UCRelaxedClockModel.Mode.categories, ucRelaxedClockModel.mode);

        // if (meanRate == null) then 1.0
        assertEquals(1.0, ucRelaxedClockModel.getMeanRate().get(), "");

        double r = ucRelaxedClockModel.getRateForBranch(tree.getRoot());
        assertEquals(1.0, r, "R = r * scale * meanRate");
    }

    //TODO more ?
}