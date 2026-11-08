package beast.base.evolution.operator.kernel;

import java.text.DecimalFormat;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.tree.Node;
import beast.base.inference.operator.kernel.KernelDistribution;

/**
 * Subtree-slide move that proposes the moved node's height by scaling, in log
 * space, the branch between the node and the subtree it carries.
 *
 * <p>When a node {@code p} is slid, it carries the subtree rooted at one of its
 * children ({@code i}); that subtree's heights do not change, so {@code i}'s
 * height is a hard floor for {@code p}. {@link beast.base.evolution.operator.kernel.BactrianSubtreeSlide}
 * proposes {@code p}'s height additively, so a downward step routinely overshoots
 * this floor and is flat-rejected ({@code NEGATIVE_INFINITY}); with a large step
 * size that flat-rejection dominates and the operator cannot be auto-tuned into a
 * useful regime.</p>
 *
 * <p>This operator instead proposes
 * <pre>
 *   gap'    = gap * exp(size * kernel)        // kernel is a mean-0 Bactrian draw
 *   height' = floor + gap'
 * </pre>
 * where {@code gap = p.height - floor > 0}. Because {@code gap' > 0} for any draw,
 * the move can never put {@code p} below the carried subtree, so that source of
 * rejection is removed by construction. The proposal is also scale-invariant with
 * respect to branch length, so a single {@code size} works across branches of very
 * different lengths and the operator auto-optimises cleanly.</p>
 *
 * <h3>Hastings ratio</h3>
 * <p>The proposal factorises into the continuous height move and the discrete
 * reattachment, so {@code logHR = log(scale) + combinatorial}, where
 * {@code log(scale) = log(gap'/gap)} is the standard scale-operator Jacobian and the
 * combinatorial term ({@code -log(#sources)} up, {@code +log(#destinations)} down,
 * {@code 0} for a pure node-age move) is identical to {@code BactrianSubtreeSlide}. Everything but
 * the height proposal is shared with it, in {@link AbstractSubtreeSlide}.</p>
 *
 * <p>Ported from beast-phylonco (commit 718de3a).</p>
 *
 * <p>Rejections from the destination side remain (e.g. in non-ultrametric trees the
 * proposed height can fall below every leaf of the reattachment subtree, leaving no
 * straddling edge); these are inherent to subtree slide and are not affected here.</p>
 *
 * <p>Not for sampled-ancestor trees. A zero-length branch above {@code i} is rejected, but if the
 * sibling {@code CiP} is a zero-length sampled-ancestor tip, {@code p} can still move and turn the
 * sampled ancestor into an ordinary tip. {@code BactrianSubtreeSlide} behaves the same way; the
 * core templates never build such trees.</p>
 */
@Description("Subtree-slide move that scales the branch above the carried subtree in log space, "
        + "so the moved node can never be proposed below the carried subtree (removing that flat rejection) "
        + "and the step is scale-invariant w.r.t. branch length, making it easier to auto-optimise than "
        + "an additive subtree slide.")
public class BactrianSubtreeScale extends AbstractSubtreeSlide {

    final public Input<Double> sizeInput = new Input<>("size",
            "scale of the log-space random walk on the gap above the carried subtree, default 0.3", 0.3);
    final public Input<KernelDistribution> kernelDistributionInput = new Input<>("kernelDistribution",
            "provides sample distribution for proposals", KernelDistribution.newDefaultKernelDistribution());
    final public Input<Boolean> optimiseInput = new Input<>("optimise",
            "flag to indicate that the size is automatically changed to achieve a good acceptance rate (default true)", true);

    private double size;
    private KernelDistribution kernelDistribution;

    @Override
    public void initAndValidate() {
        size = sizeInput.get();
        kernelDistribution = kernelDistributionInput.get();
    }

    /**
     * Scales the gap between {@code p} and the carried subtree's root {@code i} in log space.
     */
    @Override
    protected HeightProposal proposeHeight(final Node i, final Node p) {
        // the carried subtree (rooted at i) sets the floor; scale the gap above it.
        final double floor = i.getHeight();
        final double oldGap = p.getHeight() - floor;
        if (oldGap <= 0.0) {
            // zero-length branch (e.g. sampled ancestor): nothing to scale
            return null;
        }

        final double logScale = kernelDistribution.getRandomDelta(0, Double.NaN, size); // size * mean-0 draw
        final double newHeight = floor + oldGap * Math.exp(logScale);
        // Mathematically newHeight > floor, but floor + oldGap * scale can round to exactly floor
        // when the gap is tiny relative to the floor (e.g. gap 1e-12 at height 1e3). Reject that
        // rather than create a zero-length branch.
        if (newHeight <= floor) {
            return null;
        }
        // logScale is the Jacobian of the scale move on the gap.
        return new HeightProposal(newHeight, logScale);
    }

    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(final double logAlpha) {
        if (optimiseInput.get()) {
            // size is a Bactrian scale: larger size means larger jumps. Tune multiplicatively
            // so acceptance too high (delta > 0) increases size, too low decreases it.
            double delta = calcDelta(logAlpha);
            delta += Math.log(size);
            size = Math.exp(delta);
        }
    }

    @Override
    public double getCoercableParameterValue() {
        return size;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        size = value;
    }

    @Override
    public double getDefaultTargetAcceptanceProbability() {
        return 0.3;
    }

    @Override
    public String getPerformanceSuggestion() {
        final double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        final double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        final double newSize = size * ratio;

        final DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try decreasing size to about " + formatter.format(newSize);
        } else if (prob > 0.40) {
            return "Try increasing size to about " + formatter.format(newSize);
        } else return "";
    }
}
