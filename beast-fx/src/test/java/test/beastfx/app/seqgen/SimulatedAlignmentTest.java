package test.beastfx.app.seqgen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.beastfx.app.seqgen.SeqGenTestUtils.*;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.tree.Tree;
import beast.base.spec.evolution.sitemodel.SiteModel;
import beast.base.util.Randomizer;
import beastfx.app.seqgen.SimulatedAlignment;

/**
 * Unit tests for {@link SimulatedAlignment}, with BEAST 3 models only.
 */
public class SimulatedAlignmentTest {

    static final String FOUR_TAXA = "((A:0.1,B:0.1):0.1,(C:0.1,D:0.1):0.1)";

    @BeforeEach
    void setUp() {
        Randomizer.setSeedReproducible(127);
    }

    static SimulatedAlignment simulate(Alignment data, Tree tree, SiteModel siteModel, int length, Object... more) {
        SimulatedAlignment sim = new SimulatedAlignment();
        Object[] args = new Object[8 + more.length];
        Object[] base = {"data", data, "tree", tree, "siteModel", siteModel, "sequencelength", length};
        System.arraycopy(base, 0, args, 0, base.length);
        System.arraycopy(more, 0, args, base.length, more.length);
        sim.initByName(args);
        return sim;
    }

    static SimulatedAlignment fourTaxa(int length, Object... more) {
        Alignment data = emptyAlignment("A", "B", "C", "D");
        return simulate(data, tree(data, FOUR_TAXA), siteModel(jukesCantor()), length, more);
    }

    @Test
    void isAnAlignment() {
        SimulatedAlignment sim = fourTaxa(500);

        assertEquals(4, sim.getTaxonCount());
        assertEquals(java.util.List.of("A", "B", "C", "D"), sim.getTaxaNames());
        assertEquals(500, sim.getSiteCount());
        assertTrue(sim.getPatternCount() > 1 && sim.getPatternCount() <= 500);
        for (String taxon : sim.getTaxaNames()) {
            assertTrue(sim.getSequenceAsString(taxon).matches("[ACGT]{500}"));
        }
    }

    @Test
    void distanceMatchesJukesCantor() {
        int n = 100_000;
        double d = 0.5;
        Alignment data = emptyAlignment("A", "B");
        SimulatedAlignment sim = simulate(data, tree(data, "(A:" + d / 2 + ",B:" + d / 2 + ")"),
                siteModel(jukesCantor()), n);

        double expected = jukesCantorP(d);
        double p = pDistance(sim.getSequenceAsString("A"), sim.getSequenceAsString("B"));
        assertEquals(expected, p, 4 * se(expected, n));
    }

    @Test
    void labelledTreeMapsTaxaByName() {
        // tree tips in a different order from the alignment: A-C and B-D are the close pairs
        Alignment data = emptyAlignment("A", "B", "C", "D");
        Tree tree = tree(data, "((C:0.001,A:0.001):1.0,(D:0.001,B:0.001):1.0)");
        SimulatedAlignment sim = simulate(data, tree, siteModel(jukesCantor()), 5000);

        assertTrue(pDistance(sim.getSequenceAsString("A"), sim.getSequenceAsString("C")) < 0.02, "A and C are sisters");
        assertTrue(pDistance(sim.getSequenceAsString("B"), sim.getSequenceAsString("D")) < 0.02, "B and D are sisters");
        assertTrue(pDistance(sim.getSequenceAsString("A"), sim.getSequenceAsString("B")) > 0.4, "A and B are far apart");
    }

    @Test
    void globalSeedIsReproducible() {
        Randomizer.setSeedReproducible(5);
        Map<String, String> first = sequences(fourTaxa(1000));
        Randomizer.setSeedReproducible(5);
        Map<String, String> again = sequences(fourTaxa(1000));
        Randomizer.setSeedReproducible(6);
        Map<String, String> other = sequences(fourTaxa(1000));

        assertEquals(first, again);
        assertNotEquals(first, other);
    }

    @Test
    void localSeedIsReproducible() {
        Randomizer.setSeedReproducible(5);
        Map<String, String> first = sequences(fourTaxa(1000, "seed", 42L));
        Randomizer.setSeedReproducible(6);
        Map<String, String> again = sequences(fourTaxa(1000, "seed", 42L));
        Map<String, String> other = sequences(fourTaxa(1000, "seed", 43L));

        assertEquals(first, again, "the local seed alone should decide the alignment");
        assertNotEquals(first, other);
    }

    /** Draws from the global random stream, e.g. those an MCMC would make after the alignment is built. */
    static double[] draws(int n) {
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = Randomizer.nextDouble();
        }
        return x;
    }

    @Test
    void localSeedLeavesGlobalStreamAlone() {
        Randomizer.setSeedReproducible(5);
        double[] reference = draws(6);

        Randomizer.setSeedReproducible(5);
        double[] before = draws(3);
        fourTaxa(1000, "seed", 42L);
        double[] after = draws(3);

        // a local seed should not disturb the global stream: it continues where it was
        assertArrayEquals(new double[] {reference[0], reference[1], reference[2]}, before);
        assertArrayEquals(new double[] {reference[3], reference[4], reference[5]}, after);
    }

    @Test
    void globalSeedStreamIsNotReplayed() {
        Randomizer.setSeedReproducible(5);
        double[] reference = draws(5);

        Randomizer.setSeedReproducible(5);
        fourTaxa(1000);
        double[] after = draws(5);

        // without a local seed the simulation uses the global stream, so what follows must not
        // repeat the numbers the simulation already used
        assertNotEquals(reference[0], after[0], "the global stream was restarted after the simulation");
    }

    @Test
    void outputFileWritten(@TempDir Path dir) throws Exception {
        File out = dir.resolve("simulated.xml").toFile();
        SimulatedAlignment sim = fourTaxa(300, "outputFileName", out.getPath());

        assertTrue(out.exists());
        // The file describes the SimulatedAlignment itself (tree, site model and the simulated sequences),
        // so parsing it back would simulate again. Check that the simulated sequences are in it.
        String xml = java.nio.file.Files.readString(out.toPath());
        assertTrue(xml.contains("SimulatedAlignment"));
        for (String taxon : sim.getTaxaNames()) {
            assertTrue(xml.contains(sim.getSequenceAsString(taxon)), "output should contain the sequence of " + taxon);
        }
    }
}
