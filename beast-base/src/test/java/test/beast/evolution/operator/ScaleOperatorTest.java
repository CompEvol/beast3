package test.beast.evolution.operator;

import beast.base.evolution.operator.ScaleOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.TreeParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
/**
 * @deprecated replaced by {@link beast.base.spec.evolution.operator.ScaleOperatorTest}
 */
@Deprecated
public class ScaleOperatorTest  {
	final static double EPSILON = 1e-10;

	
	@Test
	public void testTreeScaling() {
        String newick = "((0:1.0,1:1.0)4:1.0,(2:1.0,3:1.0)5:0.5)6:0.0;";

        TreeParser tree = new TreeParser(newick, false, false, false, 0);

        Node [] node = tree.getNodesAsArray();
        
        ScaleOperator operator = new ScaleOperator();
        operator.initByName("tree", tree, "weight", 1.0);
        operator.proposal();
        
        // leaf node
        node = tree.getNodesAsArray();
        assertEquals(0.0, node[0].getHeight(), EPSILON);
        assertEquals(0.0, node[1].getHeight(), EPSILON);
        // leaf node, not scaled
        assertEquals(0.5, node[2].getHeight(), EPSILON);
        assertEquals(0.5, node[3].getHeight(), EPSILON);
        
        // Under interval-scaling Tree.scale:
        //  * node4 (parent of leaves at h=0):    margin 1.0 -> 1.0*s, new h = s
        //  * node5 (parent of leaves at h=0.5):  margin 1.0 -> 1.0*s, new h = 0.5 + s
        //  * node6 (root, children node4=s, node5=0.5+s):
        //       old margin = 2.0 - 1.5 = 0.5; after recurse min child h = 0.5 + s
        //       new h = (0.5 + s) + 0.5*s = 0.5 + 1.5*s
        // Recover s from node4 (whose taller child is a leaf at h=0).
        double scale = node[4].getHeight();
        assertEquals(scale,             node[4].getHeight(), EPSILON);
        assertEquals(0.5 + scale,       node[5].getHeight(), EPSILON);
        assertEquals(0.5 + 1.5 * scale, node[6].getHeight(), EPSILON);
	}
}
