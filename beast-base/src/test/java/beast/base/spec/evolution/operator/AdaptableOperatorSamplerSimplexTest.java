package beast.base.spec.evolution.operator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import beast.base.inference.OperatorSchedule;
import beast.base.spec.inference.operator.DeltaExchangeOperator;
import beast.base.spec.inference.parameter.SimplexParam;
import beast.base.util.Randomizer;

/**
 * AdaptableOperatorSampler must learn operator weights for a simplex parameter (beast3#179).
 * The mean of a simplex's elements is always 1/K, so normalising by its variance gave 0/0
 * and the sampler fell back to picking operators uniformly.
 */
public class AdaptableOperatorSamplerSimplexTest {

    static DeltaExchangeOperator deltaExchange(String id, SimplexParam p, double delta) {
        DeltaExchangeOperator op = new DeltaExchangeOperator();
        op.setID(id);
        op.initByName("parameter", p, "delta", delta, "autoOptimize", false, "weight", 1.0);
        return op;
    }

    @Test
    public void learnsWeightsForSimplex() {
        Randomizer.setSeed(127);
        SimplexParam freqs = new SimplexParam(new double[]{0.25, 0.25, 0.25, 0.25});
        freqs.setID("freqs");
        DeltaExchangeOperator big = deltaExchange("big", freqs, 0.1);
        DeltaExchangeOperator tiny = deltaExchange("tiny", freqs, 1e-6);

        AdaptableOperatorSampler sampler = new AdaptableOperatorSampler();
        sampler.setID("sampler");
        sampler.initByName("parameter", freqs, "operator", big, "operator", tiny,
                "burnin", 0, "learnin", 200, "weight", 1.0);
        OperatorSchedule schedule = new OperatorSchedule();
        schedule.initByName("autoOptimize", false);
        sampler.setOperatorSchedule(schedule);

        // Target uniform on the simplex: every valid proposal is accepted (DeltaExchange is symmetric)
        for (int i = 0; i < 20000; i++) {
            double logHR = sampler.proposal();
            if (Double.isFinite(logHR)) {
                sampler.accept();
            } else {
                // DeltaExchange leaves the values unchanged when it returns -infinity
                sampler.reject(-1);
            }
        }

        double[] cum = sampler.getOperatorCumulativeProbs(true);
        double pBig = cum[0];
        // the tiny operator moves the simplex about 1e5 times less, so nearly all weight goes to big
        assertTrue(pBig > 0.9, "weight of the useful operator should be close to 1, got " + pBig);
    }
}
