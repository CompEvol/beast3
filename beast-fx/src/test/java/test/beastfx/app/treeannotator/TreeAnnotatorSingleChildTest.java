package test.beastfx.app.treeannotator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beastfx.app.treeannotator.TreeAnnotator;

/**
 * Trees with unnamed single-child nodes, such as SIEVE trees with a root above a trunk:
 * TreeAnnotator's tree sets collapse them as trees are read (#333).
 */
class TreeAnnotatorSingleChildTest {

    @TempDir
    Path dir;

    private static final String[] BINARY = {
            "(((1:1,2:1)[&rate=0.5]:1,3:2):1,(4:1,5:1):2)",
            "(((1:1,3:1):1,2:2):1,(4:1,5:1):2)",
            "((1:2,(2:1,3:1):1):1,(4:1,5:1):2)",
    };

    /** A NEXUS file of the trees in BINARY, or of each under a single-child root (one also with one inside). */
    private Path nexus(String name, boolean singleChild) throws IOException {
        StringBuilder b = new StringBuilder("#NEXUS\n\nBegin taxa;\n\tDimensions ntax=5;\n\tTaxlabels A B C D E;\nEnd;\n"
                + "Begin trees;\n\tTranslate\n\t\t1 A,\n\t\t2 B,\n\t\t3 C,\n\t\t4 D,\n\t\t5 E\n;\n");
        for (int i = 0; i < BINARY.length; i++) {
            String t = BINARY[i];
            if (singleChild) {
                if (i == 1) {
                    t = t.replace("(1:1,3:1):1", "((1:1,3:1):0.5):0.5");
                }
                t = "(" + t + ":0.5)[&rate=2.0]";
            }
            b.append("tree STATE_").append(i).append(" = ").append(t).append(":0.0;\n");
        }
        b.append("End;\n");
        Path file = dir.resolve(name + ".trees");
        Files.writeString(file, b.toString());
        return file;
    }

    private static List<Tree> read(TreeAnnotator.TreeSet set) throws IOException {
        List<Tree> trees = new ArrayList<>();
        set.reset();
        while (set.hasNext()) {
            trees.add(set.next());
        }
        return trees;
    }

    private static List<TreeAnnotator.TreeSet> treeSets(Path file) throws IOException {
        TreeAnnotator annotator = new TreeAnnotator();
        return List.of(annotator.new FastTreeSet(file.toString(), 0),
                annotator.new MemoryFriendlyTreeSet(file.toString(), 0));
    }

    @Test
    void treeSetsCollapseUnnamedSingleChildNodes() throws IOException {
        List<TreeAnnotator.TreeSet> binarySets = treeSets(nexus("binary", false));
        List<TreeAnnotator.TreeSet> singleSets = treeSets(nexus("single", true));
        for (int s = 0; s < 2; s++) {
            List<Tree> binary = read(binarySets.get(s));
            List<Tree> collapsed = read(singleSets.get(s));
            assertEquals(binary.size(), collapsed.size());
            for (int i = 0; i < binary.size(); i++) {
                Tree b = binary.get(i), c = collapsed.get(i);
                String what = singleSets.get(s).getClass().getSimpleName() + " tree " + i;
                assertEquals(b.getNodeCount(), c.getNodeCount(), what + ": node count");
                for (Node node : c.getNodesAsArray()) {
                    assertTrue(node.getChildCount() == 0 || node.getChildCount() == 2, what + ": binary");
                }
                for (int n = 0; n < c.getNodeCount(); n++) {
                    assertEquals(n, c.getNode(n).getNr(), what + ": node numbering");
                }
                assertEquals(c.getNodeCount() - 1, c.getRoot().getNr(), what + ": root last");
                assertEquals(b.getRoot().getHeight(), c.getRoot().getHeight(), 1e-12, what + ": root is the old root's child");
                assertEquals(b.getRoot().toNewick(), c.getRoot().toNewick(), what + ": same tree");
                assertEquals(b.getID(), c.getID());
            }
            // metadata of the kept nodes is kept
            Node ab = collapsed.get(0).getRoot().getChild(0).getChild(0);
            assertEquals(0.5, ((Number) ab.getMetaData("rate")).doubleValue());
        }
    }

    @Test
    void namedSingleChildNodeIsRefused() throws IOException {
        Path file = dir.resolve("named.trees");
        Files.writeString(file, "#NEXUS\nBegin trees;\ntree STATE_0 = (((A:1,B:1):1)F:1,(C:1,(D:0.5,E:0.5):0.5):1):0.0;\nEnd;\n");
        TreeAnnotator annotator = new TreeAnnotator();
        TreeAnnotator.TreeSet set = annotator.new MemoryFriendlyTreeSet(file.toString(), 0);
        set.reset();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, set::next);
        assertTrue(e.getMessage().contains("'F'") && e.getMessage().contains("sampled ancestor"), e.getMessage());
    }
}
