package test.beastfx.app.seqgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.beastfx.app.seqgen.SeqGenTestUtils.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.tree.Tree;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.evolution.branchratemodel.StrictClockModel;
import beast.base.spec.evolution.sitemodel.SiteModel;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.util.Randomizer;
import beastfx.app.seqgen.MergeDataWith;
import beastfx.app.seqgen.SequenceSimulator;

/**
 * Unit tests for {@link SequenceSimulator}, with BEAST 3 models only.
 */
public class SequenceSimulatorTest {

    static final String FOUR_TAXA = "((A:0.1,B:0.1):0.1,(C:0.1,D:0.1):0.1)";

    @BeforeEach
    void setUp() {
        Randomizer.setSeedReproducible(127);
    }

    static SequenceSimulator simulator(Alignment data, Tree tree, SiteModel siteModel, int length, Object... more) {
        SequenceSimulator sim = new SequenceSimulator();
        Object[] args = new Object[8 + more.length];
        Object[] base = {"data", data, "tree", tree, "siteModel", siteModel, "sequencelength", length};
        System.arraycopy(base, 0, args, 0, base.length);
        System.arraycopy(more, 0, args, base.length, more.length);
        sim.initByName(args);
        return sim;
    }

    /** p-distance between A and B on the 2-taxon tree (A:d/2,B:d/2). */
    static double simulatedP(double d, SiteModel siteModel, int length, Object... more) {
        Alignment data = emptyAlignment("A", "B");
        Tree tree = tree(data, "(A:" + d / 2 + ",B:" + d / 2 + ")");
        Map<String, String> seqs = sequences(simulator(data, tree, siteModel, length, more).simulate());
        return pDistance(seqs.get("A"), seqs.get("B"));
    }

    @Test
    void outputShape() {
        Alignment data = emptyAlignment("A", "B", "C", "D");
        Alignment out = simulator(data, tree(data, FOUR_TAXA), siteModel(jukesCantor()), 500).simulate();

        Map<String, String> seqs = sequences(out);
        assertEquals(java.util.Set.of("A", "B", "C", "D"), seqs.keySet());
        for (String s : seqs.values()) {
            assertEquals(500, s.length());
            assertTrue(s.matches("[ACGT]+"), "only nucleotides expected: " + s.substring(0, 20));
        }
        assertEquals("nucleotide", out.userDataTypeInput.get().getTypeDescription());
    }

    @Test
    void sameSeedSameAlignment() {
        Alignment data = emptyAlignment("A", "B", "C", "D");
        Tree tree = tree(data, FOUR_TAXA);
        SequenceSimulator sim = simulator(data, tree, siteModel(jukesCantor()), 1000);

        Randomizer.setSeedReproducible(1);
        Map<String, String> first = sequences(sim.simulate());
        Randomizer.setSeedReproducible(1);
        Map<String, String> again = sequences(sim.simulate());
        Randomizer.setSeedReproducible(2);
        Map<String, String> other = sequences(sim.simulate());

        assertEquals(first, again);
        assertNotEquals(first, other);
    }

    @Test
    void zeroRateGivesIdenticalSequences() {
        Alignment data = emptyAlignment("A", "B", "C", "D");
        SiteModel slow = new SiteModel();
        slow.initByName("gammaCategoryCount", 1, "substModel", jukesCantor(),
                "mutationRate", new RealScalarParam<>(1e-9, PositiveReal.INSTANCE));
        Map<String, String> seqs = sequences(simulator(data, tree(data, FOUR_TAXA), slow, 2000).simulate());

        assertEquals(1, seqs.values().stream().distinct().count(), "all leaves should carry the root sequence");
    }

    @Test
    void distanceMatchesJukesCantor() {
        int n = 100_000;
        for (double d : new double[] {0.05, 0.5, 1.5}) {
            double expected = jukesCantorP(d);
            double p = simulatedP(d, siteModel(jukesCantor()), n);
            assertEquals(expected, p, 4 * se(expected, n), "p-distance for d=" + d);
        }
    }

    @Test
    void stationaryFrequencies() {
        double[] freqs = {0.4, 0.3, 0.2, 0.1};
        Alignment data = emptyAlignment("A", "B", "C", "D");
        SiteModel sm = siteModel(hky(2.0, freqs));
        Map<String, String> seqs = sequences(simulator(data, tree(data, FOUR_TAXA), sm, 50_000).simulate());

        double[] observed = baseFrequencies(seqs.values());
        for (int i = 0; i < 4; i++) {
            assertEquals(freqs[i], observed[i], 0.01, "frequency of " + "ACGT".charAt(i));
        }
    }

    @Test
    void gammaCategoriesUsed() {
        int n = 50_000;
        double plain = simulatedP(1.0, siteModel(jukesCantor()), n);
        double gamma = simulatedP(1.0, gammaSiteModel(jukesCantor(), 4, 0.1), n);
        // strong rate heterogeneity at the same mean rate gives fewer observed differences
        assertTrue(gamma < plain - 0.05, "gamma p=" + gamma + " should be well below plain p=" + plain);
    }

    @Test
    void branchRateModelScalesDistance() {
        int n = 100_000;
        StrictClockModel clock = new StrictClockModel();
        clock.initByName("clock.rate", new RealScalarParam<>(2.0, PositiveReal.INSTANCE));

        double withClock = simulatedP(0.3, siteModel(jukesCantor()), n, "branchRateModel", clock);
        double expected = jukesCantorP(0.6);
        assertEquals(expected, withClock, 4 * se(expected, n), "rate 2 should double the distance");
    }

    @Test
    void labelledTreeMapsTaxaByName() {
        // tree tips in a different order from the alignment: A-C and B-D are the close pairs
        Alignment data = emptyAlignment("A", "B", "C", "D");
        Tree tree = tree(data, "((C:0.001,A:0.001):1.0,(D:0.001,B:0.001):1.0)");
        Map<String, String> seqs = sequences(simulator(data, tree, siteModel(jukesCantor()), 5000).simulate());

        assertTrue(pDistance(seqs.get("A"), seqs.get("C")) < 0.02, "A and C are sisters");
        assertTrue(pDistance(seqs.get("B"), seqs.get("D")) < 0.02, "B and D are sisters");
        assertTrue(pDistance(seqs.get("A"), seqs.get("B")) > 0.4, "A and B are far apart");
    }

    @Test
    void runWritesOutputFile(@TempDir Path dir) throws Exception {
        Alignment data = emptyAlignment("A", "B", "C", "D");
        File out = dir.resolve("sim.xml").toFile();
        simulator(data, tree(data, FOUR_TAXA), siteModel(jukesCantor()), 200,
                "outputFileName", out.getPath()).run();

        String xml = Files.readString(out.toPath());
        for (String taxon : new String[] {"A", "B", "C", "D"}) {
            assertTrue(xml.contains("taxon=\"" + taxon + "\"") || xml.contains("taxon='" + taxon + "'"),
                    "output should contain taxon " + taxon);
        }
        assertFalse(xml.contains("value=\"?\""), "output should contain simulated, not empty, sequences");
    }

    @Test
    void iterationsAndMerge(@TempDir Path dir) throws Exception {
        Path template = dir.resolve("template.xml");
        // MergeDataWith looks for the alignment inside the parsed object, as in an analysis XML
        Files.writeString(template,
                "<beast version='2.0' namespace='beast.base.evolution.alignment:beast.base.evolution.tree'>\n"
                + "  <tree id='tree' spec='TreeParser' IsLabelledNewick='true' newick='(A:0.1,B:0.1)'>\n"
                + "    <taxa id='alignment' spec='Alignment' dataType='nucleotide'>\n"
                + "      <sequence spec='Sequence' taxon='A' value='?'/>\n"
                + "      <sequence spec='Sequence' taxon='B' value='?'/>\n"
                + "    </taxa>\n"
                + "  </tree>\n"
                + "</beast>\n");
        MergeDataWith merge = new MergeDataWith();
        merge.initByName("template", template.toString(), "output", dir.resolve("merged$(n).xml").toString());

        Alignment data = emptyAlignment("A", "B");
        simulator(data, tree(data, "(A:0.1,B:0.1)"), siteModel(jukesCantor()), 100,
                "outputFileName", dir.resolve("sim.xml").toString(), "iterations", 3, "merge", merge).run();

        for (int i = 0; i < 3; i++) {
            Path merged = dir.resolve("merged" + i + ".xml");
            assertTrue(Files.exists(merged), "missing " + merged.getFileName());
            String xml = Files.readString(merged);
            assertTrue(xml.matches("(?s).*[ACGT]{100}.*"), "merged file " + i + " should hold a simulated sequence");
        }
    }
}
