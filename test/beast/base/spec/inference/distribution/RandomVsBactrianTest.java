package beast.base.spec.inference.distribution;

import beast.base.inference.operator.kernel.KernelDistribution;
import beast.base.util.Randomizer;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RandomVsBactrianTest {

    final double windowSize = 0.1;
    KernelDistribution kernelDistribution;

    final int size = 100000;
    double[] valuesRandom;
    double[] valuesBactrian;

    final double startValue = 1.0;
    final double lower = 0.0;
    final double upper = 2.0;
    final int n = 100;

    @BeforeEach
    public void setUp() {
        valuesRandom = new double[size];
        valuesBactrian = new double[size];

        kernelDistribution = KernelDistribution.newDefaultKernelDistribution();
    }

    @Test
    void testDistWithBounds() {
        double[] ks = new double[n];
        double[] means1 = new double[n];
        double[] means2 = new double[n];

        for (int t = 0; t < n; t++) {
            valuesRandom[0] = proposeRandom(startValue);
            valuesBactrian[0] = proposeBactrian(startValue);

            for (int i = 1; i < size; i++) {
                do {
                    valuesRandom[i] = proposeRandom(valuesRandom[i - 1]);
                } while (valuesRandom[i] < lower || valuesRandom[i] > upper);
                do {
                    valuesBactrian[i] = proposeBactrian(valuesBactrian[i - 1]);
                } while (valuesBactrian[i] > upper || valuesBactrian[i] < lower);
            }
            System.out.println("******* Randomizer " + t + " ********\n");
            means1[t] = printReport(valuesRandom);
            System.out.println("******* Bactrian " + t + " ********\n");
            means2[t] = printReport(valuesBactrian);

            KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();
            double stats = ksTest.kolmogorovSmirnovStatistic(valuesRandom, valuesBactrian);
            System.out.println("two-sample Kolmogorov-Smirnov test statistic: " + stats);

            double pValue = ksTest.kolmogorovSmirnovTest(valuesRandom, valuesBactrian);
            System.out.println("pValue: " + pValue);

            ks[t] = stats;
        }

        System.out.println("\n******* Kolmogorov-Smirnov test stats ********\n");
        System.out.println("mean of KS = " + StatUtils.mean(ks) + ", var of KS = " + StatUtils.variance(ks));
        System.out.println("min KS = " + StatUtils.min(ks) + ", max KS = " + StatUtils.max(ks));
        System.out.println();

        System.out.println("Randomizer mean of means = " + StatUtils.mean(means1) + ", var of means = " + StatUtils.variance(means1) );
        System.out.println("Bactrian mean of means = " + StatUtils.mean(means2) + ", var of means = " + StatUtils.variance(means2) );

        // For two-sample KS < 0.05, interpret as practically indistinguishable
        assertTrue(StatUtils.mean(ks) < 0.05 );
    }


    private double proposeRandom(final double value) {
        return value + Randomizer.nextDouble() * 2 * windowSize - windowSize;
    }

    private double proposeBactrian(final double value) {
        return value + kernelDistribution.getRandomDelta(0, value, windowSize);
    }

    private double printReport(final double[] values) {
        double min = StatUtils.min(values);
        double max = StatUtils.max(values);
        double mean = StatUtils.mean(values);
        double var = StatUtils.variance(values);

        System.out.println("mean = " + mean + ", var = " + var);
        System.out.println("min = " + min + ", max = " + max);
        System.out.println();

        return mean;
    }


//    @Test
//    void testDistribution() {
//        final double startValue = 1.0;
//        valuesRandom[0] = proposeRandom(startValue);
//        valuesBactrian[0] = proposeBactrianRandom(startValue);
//
//        for (int i = 1; i < size; i++) {
//            valuesRandom[i] = proposeRandom(valuesRandom[i-1]);
//            valuesBactrian[i] = proposeBactrianRandom(valuesBactrian[i-1]);
//        }
//        System.out.println("******* Randomizer ********\n");
//        printReport(valuesRandom);
//        System.out.println("******* Bactrian ********\n");
//        printReport(valuesBactrian);
//
//        KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();
//        double stats = ksTest.kolmogorovSmirnovStatistic(valuesRandom, valuesBactrian);
//        System.out.println("two-sample Kolmogorov-Smirnov test statistic: " + stats);
//
//        double pValue = ksTest.kolmogorovSmirnovTest(valuesRandom, valuesBactrian);
//        System.out.println("pValue: " + pValue);
//
//    }
}