package beast.base.spec.evolution.tree.coalescent;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeIntervals;
import beast.base.evolution.tree.coalescent.Coalescent;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.beast.BEASTTestCase;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexei Drummond
 * @author Walter Xie
 */
public class CoalescentTest extends BEASTTestCase {
    String[] trees = new String[]{"((A:1.0,B:1.0):1.0,C:2.0);", ""}; //more trees ?
    Alignment data;
    final double pop = 10000;

    @BeforeEach
    protected void setUp() throws Exception {
        //super.setUp();
        data = getFourTaxaNoData();
    }

    @Test
    public void testConstantPopulation() throws Exception {
        // *********** 3 taxon **********
        Tree tree = getTree(data, trees[0]);
        TreeIntervals treeIntervals = new TreeIntervals();
        treeIntervals.initByName("tree", tree);

        ConstantPopulation cp = new ConstantPopulation();
        cp.initByName("popSize", new RealScalarParam<>(pop, PositiveReal.INSTANCE));

        Coalescent coal = new Coalescent();
        coal.initByName("treeIntervals", treeIntervals, "populationModel", cp);

        double logL = coal.calculateLogP();

        assertEquals(logL, -(4 / pop) - 2 * Math.log(pop), PRECISION);

        // *********** 4 taxon **********
//        tree = getTree(data, trees[1]);
//        treeIntervals = new TreeIntervals();
//        treeIntervals.initByName("tree", tree);
//
//        cp = new ConstantPopulation();
//        cp.initByName("popSize", Double.toString(pop));
//
//        coal = new Coalescent();
//        coal.initByName("treeIntervals", treeIntervals, "populationModel", cp);
//
//        logL = coal.calculateLogP();
//
//        assertEquals(logL, -(4 / pop) - 2 * Math.log(pop), PRECISION);

    }

    public void testExponentialGrowth() throws Exception {

    }

}
