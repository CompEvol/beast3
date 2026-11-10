package beast.base.spec.evolution.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import beast.base.evolution.tree.TreeParser;
import beast.base.inference.State;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.evolution.branchratemodel.RandomLocalClockModel;
import beast.base.spec.inference.distribution.Gamma;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.util.Randomizer;

/**
 * RandomLocalClockFlipOperator must leave the prior invariant. Sampling the prior alone (IID
 * Gamma(0.5, 2) on every rate, Poisson on the number of rate changes, fixed tree) with only this
 * operator, the chain must reproduce the known marginals:
 * <ul>
 * <li>the number of rate changes s: with uniform="true" (the default, as in BitFlipOperator) it is
 * Poisson(lambda) truncated to 0..K; with uniform="false" each of the C(K, s) configurations with s
 * changes gets weight lambda^s / s!, so P(s) is proportional to C(K, s) lambda^s / s!;</li>
 * <li>every rate, on or off, is Gamma(0.5, 2): mean 1, E[log r] = digamma(0.5) + log 2.</li>
 * </ul>
 * The on-rates are the sharp test: the move proposes them near the inherited rate, so a wrong
 * Hastings ratio pulls them away from the Gamma. As in a real analysis, a scale move on the rates
 * runs alongside; the flip move alone can't spread on-rates out when sigma is small.
 */
public class RandomLocalClockFlipOperatorTest {

    static final String NEWICK = "((((A:1,B:1):1,C:2):1,(D:1.5,E:1.5):1.5):1,(F:2,G:2):2);";
    static final double ALPHA = 0.5, THETA = 2.0;
    static final double MEAN_LOG = -1.9635100260214235 + Math.log(THETA); // digamma(0.5) + log(theta)

    /** results of one run: P(sum = k), mean and mean log of all rates and of on-rates */
    record Result(double[] pSum, double meanAll, double meanLogAll, double meanLogOn, long onSamples) {}

    static Result run(boolean multipliers, double lambda, double sigma, long steps, long seed) {
        return run(multipliers, true, lambda, sigma, steps, seed);
    }

    static Result run(boolean multipliers, boolean uniform, double lambda, double sigma, long steps, long seed) {
        Randomizer.setSeed(seed);
        TreeParser tree = new TreeParser(NEWICK, false);
        int k = tree.getNodeCount() - 1;
        boolean[] ind0 = new boolean[k];
        double[] rate0 = new double[k];
        java.util.Arrays.fill(rate0, 1.0);
        BoolVectorParam indicators = new BoolVectorParam(ind0);
        indicators.setID("indicators");
        RealVectorParam<PositiveReal> rates = new RealVectorParam<>(rate0, PositiveReal.INSTANCE);
        rates.setID("rates");
        State state = new State();
        state.initByName("stateNode", indicators, "stateNode", rates);
        state.initialise();

        RandomLocalClockModel clock = new RandomLocalClockModel();
        clock.initByName("indicators", indicators, "rates", rates, "tree", tree, "ratesAreMultipliers", multipliers);

        Gamma gamma = new Gamma();
        gamma.initByName("alpha", new RealScalarParam<>(ALPHA, PositiveReal.INSTANCE),
                "theta", new RealScalarParam<>(THETA, PositiveReal.INSTANCE));

        RandomLocalClockFlipOperator op = new RandomLocalClockFlipOperator();
        op.initByName("clock", clock, "rateDistribution", gamma, "sigma", sigma, "uniform", uniform, "weight", 1.0);

        long burnin = steps / 10, samples = 0, onSamples = 0;
        double[] sumCounts = new double[k + 1];
        double sumAll = 0, sumLogAll = 0, sumLogOn = 0;
        double logP = logPrior(indicators, rates, gamma, lambda);
        for (long step = 0; step < steps; step++) {
            boolean[] oldInd = indicators.getValues().clone();
            double[] oldRates = rates.getValues().clone();
            // alternate the flip move with a log-scale random walk on one rate, as a real chain
            // moves its rates with a scale operator; the flip move alone can't spread on-rates
            double logHR;
            if (step % 2 == 0) {
                logHR = op.proposal();
            } else {
                int i = Randomizer.nextInt(k);
                double oldRate = rates.get(i);
                double newRate = oldRate * Math.exp(0.5 * Randomizer.nextGaussian());
                rates.set(i, newRate);
                logHR = Math.log(newRate / oldRate); // Jacobian of the log-scale move
            }
            double newLogP = Double.isFinite(logHR) ? logPrior(indicators, rates, gamma, lambda) : Double.NEGATIVE_INFINITY;
            if (Double.isFinite(logHR) && Math.log(Randomizer.nextDouble()) < newLogP - logP + logHR) {
                logP = newLogP;
            } else {
                for (int i = 0; i < k; i++) {
                    indicators.set(i, oldInd[i]);
                    rates.set(i, oldRates[i]);
                }
            }
            if (step >= burnin && step % 10 == 0) {
                samples++;
                int on = 0;
                for (int i = 0; i < k; i++) {
                    double r = rates.get(i);
                    sumAll += r;
                    sumLogAll += Math.log(r);
                    if (indicators.get(i)) {
                        on++;
                        sumLogOn += Math.log(r);
                        onSamples++;
                    }
                }
                sumCounts[on]++;
            }
        }
        for (int i = 0; i <= k; i++) {
            sumCounts[i] /= samples;
        }
        return new Result(sumCounts, sumAll / (samples * k), sumLogAll / (samples * k), sumLogOn / onSamples, onSamples);
    }

    static double logPrior(BoolVectorParam indicators, RealVectorParam<PositiveReal> rates, Gamma gamma, double lambda) {
        double logP = 0;
        int on = 0;
        for (int i = 0; i < rates.size(); i++) {
            logP += gamma.logDensity(rates.get(i));
            if (indicators.get(i)) {
                on++;
            }
        }
        // Poisson(lambda) on the number of rate changes, up to a constant
        double logFact = 0;
        for (int j = 2; j <= on; j++) {
            logFact += Math.log(j);
        }
        return logP + on * Math.log(lambda) - logFact;
    }

    /** P(s): truncated Poisson if uniform, else proportional to C(k, s) lambda^s / s! */
    static double[] sumDistribution(double lambda, int k, boolean uniform) {
        double[] p = new double[k + 1];
        double total = 0, term = 1;
        for (int j = 0; j <= k; j++) {
            if (j > 0) {
                term *= (uniform ? 1.0 : (k - j + 1.0) / j) * lambda / j;
            }
            p[j] = term;
            total += term;
        }
        for (int j = 0; j <= k; j++) {
            p[j] /= total;
        }
        return p;
    }

    static void check(boolean multipliers, boolean uniform, double lambda, double sigma) {
        Result r = run(multipliers, uniform, lambda, sigma, 4_000_000, 127);
        double[] expected = sumDistribution(lambda, r.pSum().length - 1, uniform);
        for (int j = 0; j < expected.length; j++) {
            assertEquals(expected[j], r.pSum()[j], 0.01, "P(" + j + " rate changes), multipliers=" + multipliers + ", uniform=" + uniform + ", sigma=" + sigma);
        }
        assertEquals(1.0, r.meanAll(), 0.03, "mean rate, multipliers=" + multipliers);
        assertEquals(MEAN_LOG, r.meanLogAll(), 0.03, "mean log rate, multipliers=" + multipliers);
        assertTrue(r.onSamples() > 100_000, "too few on-rates sampled: " + r.onSamples());
        // on-rates mix slowly when sigma is small (seed-to-seed spread about 0.1), so the tight
        // check is at sigma = 3; at small sigma it only catches gross errors
        double tolerance = sigma >= 1.0 ? 0.04 : 0.2;
        assertEquals(MEAN_LOG, r.meanLogOn(), tolerance, "mean log of on-rates, multipliers=" + multipliers + ", uniform=" + uniform + ", sigma=" + sigma);
    }

    @Test
    public void absoluteRatesKeepPriorInvariant() {
        for (double sigma : new double[] {0.05, 0.3, 3.0}) {
            check(false, true, 1.0, sigma);
        }
        check(false, false, 0.3, 3.0);
    }

    @Test
    public void multiplierRatesKeepPriorInvariant() {
        for (double sigma : new double[] {0.05, 0.3, 3.0}) {
            check(true, true, 1.0, sigma);
        }
        check(true, false, 0.3, 3.0);
    }
}
