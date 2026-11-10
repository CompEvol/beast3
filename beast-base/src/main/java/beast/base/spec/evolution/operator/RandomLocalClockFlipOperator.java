package beast.base.spec.evolution.operator;

import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Real;
import beast.base.spec.evolution.branchratemodel.RandomLocalClockModel;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.Scalar;
import beast.base.util.Randomizer;

/**
 * Switches one rate change of a random local clock on or off, and proposes its rate in the
 * same move.
 * <p>
 * A plain bit flip switches a branch's rate change on with whatever rate it had drifted to
 * while off. Off rates don't affect the likelihood and follow their prior, so that rate is
 * usually far from what the data support, and the flip is rejected. This operator instead
 * proposes the new rate close to the rate the branch inherits at the moment, so switching on
 * hardly changes the likelihood:
 * <ul>
 * <li><b>on</b>: the new rate is c &middot; exp(&sigma; &epsilon;), &epsilon; ~ N(0, 1), where c is the
 * rate the branch inherits (absolute rates) or 1 (rates are multipliers). The old off rate is
 * discarded: the reverse move draws it from the prior on the rates.</li>
 * <li><b>off</b>: the rate is redrawn from the prior on the rates, as an off rate should be.</li>
 * </ul>
 * Hastings ratio, with q the log-normal proposal and p the prior on one rate:
 * on: p(r<sub>old</sub>) / q(r<sub>new</sub> | c); off: q(r<sub>old</sub> | c) / p(r<sub>new</sub>).
 * Both directions pick the branch uniformly, so that choice cancels. The rate the branch
 * inherits doesn't depend on its own indicator, so c is the same in both directions.
 * <p>
 * The {@code rateDistribution} must be the prior on each rate, applied to every rate, on or off
 * (as BEAUti's random local clock template does with an IID prior).
 * <p>
 * With {@code uniform} (default true) the move also includes the factor that BitFlipOperator uses
 * by default, (s + 1) / (K - s) for switching on and (K - s + 1) / s for switching off, with s the
 * number of rate changes before the move and K the number of indicators. This makes all
 * configurations with the same number of rate changes together as probable as a prior on that
 * number says, so a Poisson prior on the sum of the indicators gives a Poisson number of rate
 * changes. Use the same setting as any BitFlipOperator on the same indicators, or the two moves
 * sample different models.
 */
@Description("Switches one rate change of a random local clock on or off and proposes its rate with it: "
        + "on, close to the rate the branch inherits; off, from the prior on the rates.")
public class RandomLocalClockFlipOperator extends Operator {

    final public Input<RandomLocalClockModel> clockInput = new Input<>("clock",
            "the random local clock whose indicators and rates are proposed", Validate.REQUIRED);

    final public Input<ScalarDistribution<Scalar<Real, Double>, Double>> rateDistributionInput = new Input<>(
            "rateDistribution", "the prior on each rate of the clock, e.g. the distribution inside its IID prior",
            Validate.REQUIRED);

    final public Input<Double> sigmaInput = new Input<>("sigma",
            "standard deviation, on the log scale, of a newly switched-on rate around the rate the branch inherits. "
                    + "Works best close to the posterior spread of a local clock's log rate.",
            0.1);

    final public Input<Boolean> uniformInput = new Input<>("uniform",
            "as in BitFlipOperator: when true, the prior on the number of rate changes is the prior on that number, "
                    + "with all configurations with the same number equally probable; when false, every configuration "
                    + "gets the prior density of its number of rate changes (default true)",
            true);

    private RandomLocalClockModel clock;
    private BoolVectorParam indicators;
    private RealVectorParam<?> rates;
    private Tree tree;
    private boolean ratesAreMultipliers;
    private boolean includeRoot;
    private double sigma;
    private boolean uniform;

    @Override
    public void initAndValidate() {
        clock = clockInput.get();
        if (!(clock.indicatorParamInput.get() instanceof BoolVectorParam indicatorParam)) {
            throw new IllegalArgumentException(getID() + ": the clock's indicators must be a BoolVectorParam");
        }
        if (!(clock.rateParamInput.get() instanceof RealVectorParam<?> rateParam)) {
            throw new IllegalArgumentException(getID() + ": the clock's rates must be a RealVectorParam");
        }
        indicators = indicatorParam;
        rates = rateParam;
        tree = clock.treeInput.get();
        ratesAreMultipliers = clock.ratesAreMultipliersInput.get();
        includeRoot = clock.includeRootInput.get();
        sigma = sigmaInput.get();
        uniform = uniformInput.get();
        if (!(sigma > 0) || Double.isInfinite(sigma)) {
            throw new IllegalArgumentException(getID() + ": sigma must be positive and finite, got " + sigma);
        }
    }

    /** the indicators and rates are reached through the clock, not through inputs of this operator */
    @Override
    public List<StateNode> listStateNodes() {
        List<StateNode> list = new ArrayList<>();
        list.add(indicators);
        list.add(rates);
        return list;
    }

    @Override
    public double proposal() {
        final int count = indicators.size();
        final int index = Randomizer.nextInt(count);
        final Node node = nodeFor(index);
        if (node == null) {
            return Double.NEGATIVE_INFINITY;
        }
        final double centre = ratesAreMultipliers ? 1.0 : inheritedRate(node.getParent());
        final ScalarDistribution<Scalar<Real, Double>, Double> prior = rateDistributionInput.get();
        final double oldRate = rates.get(index);
        int on = 0;
        if (uniform) {
            for (int i = 0; i < count; i++) {
                if (indicators.get(i)) {
                    on++;
                }
            }
        }

        try {
            if (!indicators.get(index)) {
                // switch on: propose the rate near the one the branch inherits
                final double newRate = centre * Math.exp(sigma * Randomizer.nextGaussian());
                indicators.set(index, true);
                rates.set(index, newRate);
                final double logCount = uniform ? Math.log((on + 1.0) / (count - on)) : 0.0;
                return prior.logDensity(oldRate) - logNormalLogDensity(newRate, centre) + logCount;
            } else {
                // switch off: the rate becomes an off rate, drawn from its prior
                final double newRate = prior.inverseCumulativeProbability(Randomizer.nextDouble());
                if (!(newRate > 0) || Double.isInfinite(newRate)) {
                    return Double.NEGATIVE_INFINITY;
                }
                indicators.set(index, false);
                rates.set(index, newRate);
                final double logCount = uniform ? Math.log((count - on + 1.0) / on) : 0.0;
                return logNormalLogDensity(oldRate, centre) - prior.logDensity(newRate) + logCount;
            }
        } catch (RuntimeException e) {
            // some distributions fail on extreme values (e.g. the gamma inverse CDF)
            return Double.NEGATIVE_INFINITY;
        }
    }

    /** log density of a rate under the log-normal proposal centred (in log space) on log(centre) */
    private double logNormalLogDensity(double rate, double centre) {
        final double z = (Math.log(rate) - Math.log(centre)) / sigma;
        return -Math.log(rate) - Math.log(sigma) - 0.5 * Math.log(2.0 * Math.PI) - 0.5 * z * z;
    }

    /**
     * The unscaled rate on the branch above {@code node} as RandomLocalClockModel computes it with
     * absolute rates: the rate of the nearest ancestor-or-self whose indicator is on, else the root
     * rate. Called with the parent of the proposed branch, so it doesn't depend on that branch's
     * own indicator.
     */
    private double inheritedRate(Node node) {
        while (node != null && !node.isRoot()) {
            final int i = indexOf(node);
            if (indicators.get(i)) {
                return rates.get(i);
            }
            node = node.getParent();
        }
        return includeRoot ? rates.get(indexOf(tree.getRoot())) : 1.0;
    }

    /** the indicator and rate index of a node, as in RandomLocalClockModel.getNr */
    private int indexOf(Node node) {
        final int nr = node.getNr();
        return nr > tree.getRoot().getNr() ? nr - 1 : nr;
    }

    /** the non-root node with indicator index {@code index} */
    private Node nodeFor(int index) {
        final Node root = tree.getRoot();
        for (Node node : tree.getNodesAsArray()) {
            if (node != root && indexOf(node) == index) {
                return node;
            }
        }
        return null;
    }
}
