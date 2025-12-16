package beast.base.spec.evolution.tree.coalescent;


import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeIntervals;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.IntSimplexParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.IntSimplex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;



/**
 * @author Alexei Drummond
 */
public class BayesianSkylineTest  {

    @Test
    public void testSkyline() throws Exception {

        //RealParameter popSize = new RealParameter("1.0", 0.0, 10.0, 2);
        //IntegerParameter groupSize = new IntegerParameter("2", 1, 4, 2);

        //popSize.setValue(1, 2.0);

        Tree tree = new Tree("(((1:1,2:1):2.5,(3:1.5,4:1.5):2):2,5:5.5);");
        TreeIntervals intervals = new TreeIntervals(tree);

        BayesianSkyline skyline = new BayesianSkyline();
        int events = tree.getInternalNodeCount();
        IntSimplex<PositiveInt> groupSizes = new IntSimplexParam<>(
                new int[]{2, 2}, PositiveInt.INSTANCE,
                events); // , 1, Integer.MAX_VALUE);

        //skyline.init(popSize, groupSize, intervals);
        skyline.initByName("popSizes", new RealVectorParam<>(new double[]{1.0,2.0}, PositiveReal.INSTANCE),
                "groupSizes", groupSizes,
                "treeIntervals", intervals);

        assertEquals(skyline.getPopSize(0.01), 1.0);
        assertEquals(skyline.getPopSize(1.49), 1.0);
        assertEquals(skyline.getPopSize(1.51), 2.0);
        assertEquals(skyline.getPopSize(5.51), 2.0);

    }

}
