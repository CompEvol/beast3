package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.IntScalar;
import beast.base.spec.type.RealScalar;
import org.apache.commons.numbers.gamma.LogBeta;
import org.apache.commons.numbers.gamma.RegularizedBeta;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.statistics.distribution.DiscreteDistribution;
import org.apache.commons.statistics.distribution.GammaDistribution;
import org.apache.commons.statistics.distribution.PoissonDistribution;

import java.util.List;


/**
 * Negative binomial distribution parameterised by its mean and a dispersion r,
 * as R's {@code dnbinom(k, size = r, mu = mean)}: a Poisson whose rate is
 * Gamma distributed with shape r and mean {@code mean}. The variance is
 * {@code mean + mean^2 / r}, so a small r means a long right tail and the
 * distribution tends to Poisson(mean) as r grows. r need not be an integer.
 */
@Description("Negative binomial distribution with mean and dispersion r, " +
        "f(k; mean, r) = Gamma(k + r) / (Gamma(r) k!) (r / (r + mean))^r (mean / (r + mean))^k " +
        "for k = 0, 1, 2, ... It has variance mean + mean^2 / r: an over-dispersed Poisson, " +
        "approaching Poisson(mean) as r grows. " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class NegativeBinomial extends ScalarDistribution<IntScalar<NonNegativeInt>, Integer> {

    final public Input<RealScalar<PositiveReal>> meanInput = new Input<>("mean",
            "mean of the distribution, defaults to 1");
    final public Input<RealScalar<PositiveReal>> rInput = new Input<>("r",
            "dispersion (also called size or shape): the variance is mean + mean^2 / r; defaults to 1");

    private MeanDispersion dist = new MeanDispersion(1.0, 1.0);
    private DiscreteDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public NegativeBinomial() {
    }

    public NegativeBinomial(IntScalar<NonNegativeInt> param, RealScalar<PositiveReal> mean, RealScalar<PositiveReal> r) {
        try {
            initByName("param", param, "mean", mean, "r", r);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e);
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
        super.initAndValidate();
    }

    @Override
    public void refresh() {
        double mean = (meanInput.get() != null) ? meanInput.get().get() : 1.0;
        double r = (rInput.get() != null) ? rInput.get().get() : 1.0;
        if (isNotEqual(dist.mean, mean) || isNotEqual(dist.r, r)) {
            dist = new MeanDispersion(mean, r);
            sampler = null;
        }
    }

    @Override
    public double calculateLogP() {
        logP = getApacheDistribution().logProbability(param.get());
        return logP;
    }

    @Override
    public List<Integer> sample() {
        MeanDispersion d = getApacheDistribution();
        if (sampler == null) {
            sampler = d.createSampler(rng);
        }
        return List.of(sampler.sample());
    }

    @Override
    public double getMean() {
        return getApacheDistribution().getMean();
    }

    @Override
    public boolean isIntegerDistribution() {
        return true;
    }

    @Override
    protected MeanDispersion getApacheDistribution() {
        refresh(); // parameters may have been changed by MCMC
        return dist;
    }

    /**
     * Negative binomial with mean and real dispersion r, in the form of an Apache
     * Commons Statistics distribution so that ScalarDistribution can derive the CDF,
     * quantiles and support from it. Apache's PascalDistribution only takes an integer r.
     * With p = r / (r + mean), the probability of success,
     * P(X = k) = Gamma(k + r) / (Gamma(r) k!) p^r (1 - p)^k and P(X &le; k) = I_p(r, k + 1).
     */
    static final class MeanDispersion implements DiscreteDistribution {

        final double mean;
        final double r;
        /** probability of success, r / (r + mean) */
        private final double p;
        /** log(r / (r + mean)) and log(mean / (r + mean)) */
        private final double logP;
        private final double logQ;

        MeanDispersion(double mean, double r) {
            if (!(mean > 0) || Double.isInfinite(mean)) {
                throw new IllegalArgumentException("NegativeBinomial: mean must be positive and finite, not " + mean);
            }
            if (!(r > 0) || Double.isInfinite(r)) {
                throw new IllegalArgumentException("NegativeBinomial: r must be positive and finite, not " + r);
            }
            this.mean = mean;
            this.r = r;
            this.p = r / (r + mean);
            // both stay accurate when one of mean and r is much smaller than the other
            this.logP = -Math.log1p(mean / r);
            this.logQ = -Math.log1p(r / mean);
        }

        @Override
        public double logProbability(int k) {
            if (k < 0) {
                return Double.NEGATIVE_INFINITY;
            }
            if (k == 0) {
                return r * logP;
            }
            // Gamma(k + r) / (Gamma(r) k!) = 1 / ((k + r) B(r, k + 1))
            return -Math.log(k + r) - LogBeta.value(r, k + 1.0) + r * logP + k * logQ;
        }

        @Override
        public double probability(int k) {
            return Math.exp(logProbability(k));
        }

        @Override
        public double cumulativeProbability(int k) {
            if (k < 0) {
                return 0.0;
            }
            if (k == Integer.MAX_VALUE) {
                return 1.0;
            }
            return RegularizedBeta.value(p, r, k + 1.0);
        }

        @Override
        public double survivalProbability(int k) {
            if (k < 0) {
                return 1.0;
            }
            if (k == Integer.MAX_VALUE) {
                return 0.0;
            }
            return RegularizedBeta.complement(p, r, k + 1.0);
        }

        /** the smallest k with P(X &le; k) &ge; prob */
        @Override
        public int inverseCumulativeProbability(double prob) {
            if (!(prob >= 0 && prob <= 1)) {
                throw new IllegalArgumentException("NegativeBinomial: probability must be in [0, 1], not " + prob);
            }
            if (prob == 0) {
                return 0;
            }
            if (prob == 1) {
                return Integer.MAX_VALUE;
            }
            return search(k -> cumulativeProbability(k) >= prob);
        }

        /** the smallest k with P(X &gt; k) &le; prob; precise in the upper tail */
        @Override
        public int inverseSurvivalProbability(double prob) {
            if (!(prob >= 0 && prob <= 1)) {
                throw new IllegalArgumentException("NegativeBinomial: probability must be in [0, 1], not " + prob);
            }
            if (prob == 1) {
                return 0;
            }
            if (prob == 0) {
                return Integer.MAX_VALUE;
            }
            return search(k -> survivalProbability(k) <= prob);
        }

        /** smallest k &ge; 0 for which the monotone predicate holds */
        private int search(java.util.function.IntPredicate reached) {
            if (reached.test(0)) {
                return 0;
            }
            // reached(lo) is false, find hi with reached(hi) true by doubling
            int lo = 0;
            long hi = Math.max(1L, (long) Math.ceil(mean));
            while (!reached.test((int) hi)) {
                lo = (int) hi;
                if (hi >= Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
                hi = Math.min(2 * hi, Integer.MAX_VALUE);
            }
            int h = (int) hi;
            while (h - lo > 1) {
                int mid = lo + (h - lo) / 2;
                if (reached.test(mid)) {
                    h = mid;
                } else {
                    lo = mid;
                }
            }
            return h;
        }

        @Override
        public double getMean() {
            return mean;
        }

        @Override
        public double getVariance() {
            return mean + mean * mean / r;
        }

        @Override
        public int getSupportLowerBound() {
            return 0;
        }

        @Override
        public int getSupportUpperBound() {
            return Integer.MAX_VALUE;
        }

        /** Gamma-Poisson mixture: lambda ~ Gamma(shape r, scale mean / r), k ~ Poisson(lambda) */
        @Override
        public Sampler createSampler(UniformRandomProvider rng) {
            final var gamma = GammaDistribution.of(r, mean / r).createSampler(rng);
            return () -> {
                double lambda = gamma.sample();
                if (!(lambda > 0)) {
                    return 0;
                }
                int k = PoissonDistribution.of(lambda).createSampler(rng).sample();
                return k;
            };
        }
    }

} // class NegativeBinomial
