package beast.base.spec.evolution.operator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import beast.base.inference.OperatorSchedule;
import beast.base.internal.json.JSONObject;
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

    static AdaptableOperatorSampler sampler(SimplexParam freqs, DeltaExchangeOperator big, DeltaExchangeOperator tiny) {
        AdaptableOperatorSampler sampler = new AdaptableOperatorSampler();
        sampler.setID("sampler");
        sampler.initByName("parameter", freqs, "operator", big, "operator", tiny,
                "burnin", 0, "learnin", 200, "weight", 1.0);
        OperatorSchedule schedule = new OperatorSchedule();
        schedule.initByName("autoOptimize", false);
        sampler.setOperatorSchedule(schedule);
        return sampler;
    }

    /** target uniform on the simplex: every valid proposal is accepted (DeltaExchange is symmetric) */
    static void run(AdaptableOperatorSampler sampler, int steps) {
        for (int i = 0; i < steps; i++) {
            double logHR = sampler.proposal();
            if (Double.isFinite(logHR)) {
                sampler.accept();
            } else {
                // DeltaExchange leaves the values unchanged when it returns -infinity
                sampler.reject(-1);
            }
        }
    }

    static JSONObject saved(AdaptableOperatorSampler sampler) throws Exception {
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        sampler.storeToFile(writer);
        writer.flush();
        return new JSONObject(out.toString());
    }

    /** the saved simplex statistics are restored, so a resumed run keeps its learned weights */
    @Test
    public void resumeRestoresSimplexStatistics() throws Exception {
        Randomizer.setSeed(127);
        SimplexParam freqs = new SimplexParam(new double[]{0.25, 0.25, 0.25, 0.25});
        freqs.setID("freqs");
        DeltaExchangeOperator big = deltaExchange("big", freqs, 0.1);
        DeltaExchangeOperator tiny = deltaExchange("tiny", freqs, 1e-6);
        AdaptableOperatorSampler first = sampler(freqs, big, tiny);
        run(first, 20000);
        JSONObject state = saved(first);
        assertTrue(state.has("param_elem_mean_0"), "per-element means are saved");

        AdaptableOperatorSampler resumed = sampler(freqs, deltaExchange("big", freqs, 0.1), deltaExchange("tiny", freqs, 1e-6));
        resumed.restoreFromFile(state);
        assertArrayEquals(first.getOperatorCumulativeProbs(true), resumed.getOperatorCumulativeProbs(true), 1e-12);
    }

    /**
     * A state file written before this change has no per-element means. Resuming from it must
     * work: the simplex statistics start at the current value, so the sampler picks operators
     * uniformly at first (as it did before) and learns again as the run continues.
     */
    @Test
    public void resumeFromStateFileWithoutSimplexStatistics() throws Exception {
        Randomizer.setSeed(127);
        SimplexParam freqs = new SimplexParam(new double[]{0.25, 0.25, 0.25, 0.25});
        freqs.setID("freqs");
        AdaptableOperatorSampler first = sampler(freqs, deltaExchange("big", freqs, 0.1), deltaExchange("tiny", freqs, 1e-6));
        run(first, 20000);
        JSONObject state = saved(first);
        state.remove("param_elem_mean_0");

        AdaptableOperatorSampler resumed = sampler(freqs, deltaExchange("big", freqs, 0.1), deltaExchange("tiny", freqs, 1e-6));
        resumed.restoreFromFile(state);
        // the rest of the state was restored (restoreFromFile catches its own exceptions)
        assertEquals(state.getString("numProposals"), saved(resumed).getString("numProposals"));
        assertEquals(0.5, resumed.getOperatorCumulativeProbs(true)[0], 1e-12, "uniform until the statistics are rebuilt");
        run(resumed, 20000);
        assertTrue(resumed.getOperatorCumulativeProbs(true)[0] > 0.9, "learns again after resuming");
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
