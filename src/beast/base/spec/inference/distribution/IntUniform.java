package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Int;
import beast.base.spec.type.IntScalar;
import org.apache.commons.statistics.distribution.DiscreteDistribution;
import org.apache.commons.statistics.distribution.UniformDiscreteDistribution;

import java.util.List;

/**
 * bounds do not support Inf and -Inf
 */
@Description("Uniform distribution over a given interval (including lower and upper values). " +
        "An exception is thrown if any bound is set to infinity.")
public class IntUniform extends ScalarDistribution<IntScalar<Int>, Integer> implements Bounded<Integer> {

    final public Input<IntScalar<Int>> lowerInput = new Input<>("lower",
            "lower bound on the interval, default 1.");
    final public Input<IntScalar<Int>> upperInput = new Input<>("upper",
            "upper bound on the interval, default 10.");

    // if (!Double.isFinite(upper - lower)) {
    //    throw new DistributionException("Range %s is not finite", upper - lower);
    private UniformDiscreteDistribution dist = UniformDiscreteDistribution.of(1, 10);
    private DiscreteDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public IntUniform() {}

    /**
     * Uniform distribution over a given interval (including lower and upper values)
     * @param param
     * @param lower An exception is thrown if set to -Infinity.
     * @param upper An exception is thrown if set to Infinity.
     */
    public IntUniform(IntScalar<Int> param,
                      IntScalar<Int> lower, IntScalar<Int> upper) {

        try {
            initByName("param", param, "lower", lower, "upper", upper);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
        super.initAndValidate();
    }

    /**
     * make sure internal state is up to date *
     */
    @Override
    public void refresh() {
        int lower  = (lowerInput.get() != null) ? lowerInput.get().get() : 1;
        int upper  = (upperInput.get()  != null) ? upperInput.get().get()  : 10;

        // Floating point comparison
        if (isNotEqual(dist.getSupportLowerBound(), lower)
                || isNotEqual(dist.getSupportUpperBound(), upper)) {
            dist = UniformDiscreteDistribution.of(lower, upper);
            sampler = dist.createSampler(rng);
        } else if (sampler == null) {
            // Ensure sampler exists
            sampler = dist.createSampler(rng);
        }
    }

    @Override
    public double calculateLogP() {
        logP = dist.logProbability(param.get() - (int) getOffset()); // no unboxing needed, faster
        return logP;
    }

    @Override
    protected double calcLogP(Integer value) {
        return dist.logProbability(value - (int) getOffset()); // scalar
    }

    @Override
    protected List<Integer> sample() {
        final int x = sampler.sample() + (int) getOffset();
        return List.of(x); // Returning an immutable result
    }

    @Override
    public Integer getLower() {
        return dist.getSupportLowerBound();
    }

    @Override
    public Integer getUpper() {
        return dist.getSupportUpperBound();
    }

    @Override
    public boolean lowerInclusive() {
        return true;
    }

    @Override
    public boolean upperInclusive() {
        return true;
    }
    
    @Override
    public Object getApacheDistribution() {
    	return dist;
    }
}
