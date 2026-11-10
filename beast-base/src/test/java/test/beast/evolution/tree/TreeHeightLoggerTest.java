package test.beast.evolution.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import beast.base.evolution.tree.TreeHeightLogger;
import beast.base.evolution.tree.TreeParser;

/**
 * TreeHeightLogger is kept (deprecated) so that BEAST 2 XML that uses it still loads. Its output
 * must be what BEAST 2.7 wrote: a column named after the tree, or the logger's id, and the root height.
 */
@SuppressWarnings("removal")
public class TreeHeightLoggerTest {

    static String header(TreeHeightLogger logger) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);
        logger.init(out);
        out.flush();
        return bytes.toString();
    }

    static String logged(TreeHeightLogger logger) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);
        logger.log(0, out);
        out.flush();
        return bytes.toString();
    }

    @Test
    public void logsRootHeightAsBeast2Did() {
        TreeParser tree = new TreeParser("((A:1.0,B:1.0):2.0,C:3.0);", false);
        tree.setID("Tree.t:x");

        TreeHeightLogger logger = new TreeHeightLogger();
        logger.initByName("tree", tree);
        assertEquals("Tree.t:x.height\t", header(logger));
        assertEquals("3.0\t", logged(logger));
        assertEquals(3.0, logger.getArrayValue(), 0.0);
        assertEquals(1, logger.getDimension());

        TreeHeightLogger named = new TreeHeightLogger();
        named.setID("TreeHeight");
        named.initByName("tree", tree);
        assertEquals("TreeHeight\t", header(named));
    }
}
