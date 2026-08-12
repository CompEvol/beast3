package beast.base.spec.evolution;

import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.spec.evolution.branchratemodel.Base;
import beast.base.spec.evolution.branchratemodel.StrictClockModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the spec {@link RateStatistic}, in particular the domain it declares.
 *
 * <p>The statistic exposes three elements — mean, variance, coefficient of
 * variation — through one {@code RealVector} domain, so that domain has to admit
 * every value all three can legitimately take. Variance and coefficient of
 * variation are exactly 0 whenever the branches share a rate, which is the
 * ordinary state of affairs under a strict clock, so the domain has to include
 * zero.</p>
 */
public class RateStatisticTest {

    /** Rates keyed off node number, so each branch can differ. */
    private static class FixedRates extends Base {
        private final double[] rates;
        FixedRates(double... rates) { this.rates = rates; }
        @Override public void initAndValidate() {}
        @Override public double getRateForBranch(Node node) { return rates[node.getNr()]; }
    }

    @Test
    public void strictClockLeavesVarianceAtZeroAndStillValidates() throws Exception {
        // Every branch shares a rate, so variance and coefficient of variation are
        // exactly 0. PositiveReal excludes 0, so declaring it here made isValid()
        // report false for a perfectly ordinary strict-clock analysis.
        RateStatistic rs = statisticOn(new StrictClockModel());

        double[] values = rs.calcValues();
        assertEquals(0.0, values[1], 0.0, "strict clock should give zero variance");
        assertEquals(0.0, values[2], 0.0, "strict clock should give zero coefficient of variation");

        assertTrue(rs.isValid(0.0), "the declared domain must admit zero");
        assertTrue(rs.isValid(), "a strict-clock RateStatistic must validate");
    }

    @Test
    public void domainStillRejectsNegatives() throws Exception {
        // Widening to NonNegativeReal must not weaken the domain to plain Real:
        // none of the three statistics can be negative.
        RateStatistic rs = statisticOn(new StrictClockModel());
        assertFalse(rs.isValid(-1.0), "negative values are not in the domain");
    }

    @Test
    public void varyingRatesProduceTheDocumentedStatistics() throws Exception {
        // 3 tips: A, B at height 0, C at height 0; P at 1 joins A and B; root at 2.
        // Node numbering: A=0, B=1, C=2, P=3, root=4. The root has no branch, so
        // four branches are measured: A, B, C (external) and P (internal).
        //   branch lengths: A=1, B=1, C=2, P=1
        //   rates:          A=1, B=2, C=3, P=4  (root's entry is never read)
        RateStatistic rs = statisticOn(new FixedRates(1.0, 2.0, 3.0, 4.0, 99.0));

        double[] values = rs.calcValues();
        // mean is branch-length weighted: (1*1 + 2*1 + 3*2 + 4*1) / (1+1+2+1)
        assertEquals((1.0 + 2.0 + 6.0 + 4.0) / 5.0, values[0], 1e-12,
                "mean should be weighted by branch length");
        // variance and cv are unweighted over the same four rates
        double unweightedMean = (1.0 + 2.0 + 3.0 + 4.0) / 4.0;
        double sumSq = 0.0;
        for (double r : new double[] { 1.0, 2.0, 3.0, 4.0 }) {
            sumSq += (r - unweightedMean) * (r - unweightedMean);
        }
        double expectedVariance = sumSq / 3.0;   // DiscreteStatistics uses count - 1
        assertEquals(expectedVariance, values[1], 1e-12);
        assertEquals(Math.sqrt(expectedVariance) / unweightedMean, values[2], 1e-12);

        assertTrue(rs.isValid(), "varying rates should validate too");
    }

    @Test
    public void getRejectsOutOfRangeIndices() throws Exception {
        // The legacy getArrayValue(dim) guarded with `dim > 3`, so dim == 3 fell
        // through to an ArrayIndexOutOfBoundsException and negatives went unchecked.
        RateStatistic rs = statisticOn(new StrictClockModel());
        assertEquals(3, rs.size());
        assertThrows(IllegalArgumentException.class, () -> rs.get(3));
        assertThrows(IllegalArgumentException.class, () -> rs.get(-1));
    }

    /** Three-tip ultrametric tree wired to the given branch rate model. */
    private RateStatistic statisticOn(Base branchRateModel) throws Exception {
        Node a = leaf("A", 0);
        Node b = leaf("B", 1);
        Node c = leaf("C", 2);
        Node p = internal(3, 1.0, a, b);
        Tree tree = new Tree(internal(4, 2.0, p, c));

        branchRateModel.initAndValidate();
        RateStatistic rs = new RateStatistic();
        rs.initByName("tree", tree, "branchratemodel", branchRateModel);
        return rs;
    }

    private Node leaf(String id, int nr) {
        Node n = new Node(id);
        n.setNr(nr);
        n.setHeight(0.0);
        return n;
    }

    private Node internal(int nr, double height, Node left, Node right) {
        Node n = new Node();
        n.setNr(nr);
        n.setHeight(height);
        n.addChild(left);
        n.addChild(right);
        return n;
    }
}
