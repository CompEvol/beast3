package test.beastfx.app.seqgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.evolution.sitemodel.SiteModel;
import beast.base.spec.evolution.substitutionmodel.Frequencies;
import beast.base.spec.evolution.substitutionmodel.HKY;
import beast.base.spec.evolution.substitutionmodel.JukesCantor;
import beast.base.spec.evolution.substitutionmodel.Base;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.SimplexParam;

/**
 * Shared set-up for the seqgen simulator tests. Only BEAST 3 ({@code beast.base.spec}) models are used,
 * no deprecated classes.
 */
final class SeqGenTestUtils {

    private SeqGenTestUtils() { }

    /** Nucleotide alignment with the given taxa and empty ("?") sequences, used as the data template. */
    static Alignment emptyAlignment(String... taxa) {
        List<Sequence> seqs = new ArrayList<>();
        for (String taxon : taxa) {
            seqs.add(new Sequence(taxon, "?"));
        }
        return new Alignment(seqs, "nucleotide");
    }

    /** Tree from a labelled newick string; tips are matched to the alignment's taxa by name. */
    static Tree tree(Alignment data, String newick) {
        TreeParser tree = new TreeParser();
        tree.initByName("taxa", data, "newick", newick, "IsLabelledNewick", true);
        return tree;
    }

    static SiteModel siteModel(Base substModel) {
        SiteModel sm = new SiteModel();
        sm.initByName("gammaCategoryCount", 1, "substModel", substModel);
        return sm;
    }

    static SiteModel gammaSiteModel(Base substModel, int categories, double shape) {
        SiteModel sm = new SiteModel();
        sm.initByName("gammaCategoryCount", categories, "shape", new RealScalarParam<>(shape, PositiveReal.INSTANCE),
                "substModel", substModel);
        return sm;
    }

    static JukesCantor jukesCantor() {
        JukesCantor jc = new JukesCantor();
        jc.initAndValidate();
        return jc;
    }

    static HKY hky(double kappa, double... freqs) {
        Frequencies f = new Frequencies();
        f.initByName("frequencies", new SimplexParam(freqs), "estimate", false);
        HKY hky = new HKY();
        hky.initByName("kappa", new RealScalarParam<>(kappa, PositiveReal.INSTANCE), "frequencies", f);
        return hky;
    }

    /** Sequences of an alignment by taxon name. Works on alignments that were not initialised. */
    static Map<String, String> sequences(Alignment alignment) {
        Map<String, String> map = new HashMap<>();
        for (Sequence s : alignment.sequenceInput.get()) {
            map.put(s.taxonInput.get(), s.dataInput.get());
        }
        return map;
    }

    /** Proportion of sites at which two equally long sequences differ. */
    static double pDistance(String a, String b) {
        if (a.length() != b.length()) {
            throw new IllegalArgumentException("lengths differ: " + a.length() + " vs " + b.length());
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                diff++;
            }
        }
        return (double) diff / a.length();
    }

    /** Expected p-distance under Jukes-Cantor for a distance of d substitutions per site. */
    static double jukesCantorP(double d) {
        return 0.75 * (1 - Math.exp(-4.0 * d / 3.0));
    }

    /** Standard error of a proportion p estimated from n sites. */
    static double se(double p, int n) {
        return Math.sqrt(p * (1 - p) / n);
    }

    /** Base frequencies (A, C, G, T) over all the given sequences. */
    static double[] baseFrequencies(Iterable<String> seqs) {
        double[] counts = new double[4];
        double total = 0;
        for (String s : seqs) {
            for (char c : s.toCharArray()) {
                int i = "ACGT".indexOf(Character.toUpperCase(c));
                if (i >= 0) {
                    counts[i]++;
                    total++;
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            counts[i] /= total;
        }
        return counts;
    }
}
