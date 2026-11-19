package beastfx.app.inputeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import beast.base.parser.PartitionContext;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.parameter.RealScalarParam;

/**
 * The partition context for a new value of an input (#302, #304): taken from the current
 * value, the owner or the owner's inputs, whichever names a partition of the document, and
 * never a NullPointerException for a missing value or ID.
 */
public class BeautiDocContextTest {

    private BeautiDoc doc;
    private PartitionContext dna;
    private PartitionContext rbcl;

    @BeforeEach
    void setUp() {
        doc = new BeautiDoc();
        dna = new PartitionContext("dna");
        rbcl = new PartitionContext("rbcl", "rbcl", "dnaClock", "dnaTree");
        doc.partitionNames.add(dna);
        doc.partitionNames.add(rbcl);
    }

    private static Normal normal(String id) {
        Normal n = new Normal();
        n.setID(id);
        return n;
    }

    @Test
    void valueGivesThePartition() {
        assertSame(rbcl, doc.getContextFor(normal("Prior.c:dnaClock"), normal("x.t:dna")));
    }

    @Test
    void ownerGivesThePartitionWhenThereIsNoValue() {
        assertSame(rbcl, doc.getContextFor(null, normal("clockPrior.c:dnaClock")));
    }

    @Test
    void ownerGivesThePartitionWhenTheValueHasNoID() {
        assertSame(dna, doc.getContextFor(normal(null), normal("prior.s:dna")));
    }

    /**
     * An owner whose ID names no partition, e.g. an MRCA prior {@code human-chimp.prior},
     * gets the partition of an object among its inputs, not a context "prior" that matches
     * no partition.
     */
    @Test
    void ownerWithoutPartitionUsesItsInputs() {
        Normal owner = normal("human-chimp.prior");
        RealScalarParam<Real> mean = new RealScalarParam<>(0.0, Real.INSTANCE);
        mean.setID("height.t:dnaTree");
        owner.initByName("mean", mean, "sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        assertSame(rbcl, doc.getContextFor(null, owner));
    }

    @Test
    void nothingNamesAPartition() {
        assertEquals("", doc.getContextFor(null, null).partition);
        assertEquals("", doc.getContextFor(normal(null), normal(null)).partition);
        assertEquals("", doc.getContextFor((Normal) null).partition);
        assertEquals("", doc.getContextFor(normal(null)).partition);
        // as before: an ID naming an unknown partition gives a context for that name
        assertEquals("other", doc.getContextFor(normal("x.c:other")).partition);
    }
}
