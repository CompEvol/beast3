package beast.base.spec.evolution.operator;

import org.apache.commons.math4.legacy.stat.StatUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks an MCMC sample against LogNormal(mean = 1 in real space, sigma = 1),
 * i.e. log X ~ Normal(-0.5, 1), the target used by the operator tests.
 * <p>
 * Targets are exact: mean 1, median exp(-0.5), quantiles exp(-0.5 + z_p).
 * Tolerances are about 5 times the largest run-to-run SD of each statistic, measured
 * over 30 seeds of each scale and random-walk operator test (1M to 2M steps), so a
 * correct sampler should essentially never fail.
 * <p>
 * The sample variance is not checked: for this distribution its run-to-run SD is up
 * to 0.12 (heavy right tail, fourth central moment about 336), so a useful
 * tolerance is impossible. The upper quantiles test the tail more sharply.
 */
public final class LogNormalSampleCheck {

    private static final double MU = -0.5;

    private LogNormalSampleCheck() {
    }

    public static void assertMean1Sigma1(double[] v) {
        assertEquals(1.0, StatUtils.mean(v), 0.03, "mean");
        assertEquals(Math.exp(MU), StatUtils.percentile(v, 50), 0.02, "median");
        assertEquals(Math.exp(MU - 1.959964), StatUtils.percentile(v, 2.5), 5e-3, "2.5% quantile");
        assertEquals(Math.exp(MU - 1.644854), StatUtils.percentile(v, 5), 5e-3, "5% quantile");
        assertEquals(Math.exp(MU + 1.644854), StatUtils.percentile(v, 95), 0.15, "95% quantile");
        assertEquals(Math.exp(MU + 1.959964), StatUtils.percentile(v, 97.5), 0.25, "97.5% quantile");
    }
}
