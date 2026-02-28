package beast.base.spec.evolution.substmodel;

import beast.base.evolution.tree.Node;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.evolution.substitutionmodel.Frequencies;
import beast.base.spec.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.SimplexParam;
import beast.base.spec.type.RealVector;
import beast.base.spec.type.Simplex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.DoubleStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * This tests transition probability matrix from {@link GeneralSubstitutionModel} given rates.
 * Instantaneous rate q_ij can be 0, but the transition prob p_ij(t) cannot.
 *
 * @author Walter Xie
 */
public class GeneralSubstitutionModelTest {
    GeneralSubstitutionModel geneSubstModel;

    @BeforeEach
    public void setUp() {
        Simplex f = new SimplexParam(new double[]{1.0/3.0, 1.0/3.0, 1.0/3.0});
        Frequencies freqs = new Frequencies();
        freqs.initByName("frequencies", f, "estimate", false);

        // A -> B -> C, not A -> C
        // off-diagonal: NrOfStates * (NrOfStates - 1)
        RealVector<NonNegativeReal> rates = new RealVectorParam<>(
                new double[]{0.1, 0.0, 0.1, 0.2, 0.0, 0.2}, NonNegativeReal.INSTANCE);
        geneSubstModel = new GeneralSubstitutionModel();
        geneSubstModel.initByName("frequencies", freqs, "rates", rates);
    }

    @Test
    public void getTransitionProbabilities() {
//        double startTime = 1E-10; // when genetic distance -> 1E-10, P(t) may has 0.
        double startTime = 1;
        double endTime = 0;
        double rate = 1;

        System.out.println("freqs = \n" + Arrays.toString(geneSubstModel.getFrequencies()) + "\n");

        int len = geneSubstModel.getStateCount();
        double[] prob = new double[len*len];
        geneSubstModel.getTransitionProbabilities(new Node(), startTime, endTime, rate, prob, true);

        System.out.println("relative rates :\n" +
                Arrays.toString(geneSubstModel.getRelativeRates()) + "\n");
        System.out.println("\nrenormalised rate matrix :");
        double[][] rateM = geneSubstModel.getRateMatrix();
        for(int i = 0; i < rateM.length; i++)
            System.out.println(Arrays.toString(rateM[i]));
        System.out.println("\ntransition prob :\n" + Arrays.toString(prob));

        // P(t) row sum to 1
        for (int i=0; i < len; i++) {
            double[] row = new double[len];
            System.arraycopy(prob, i*len, row, 0, len);
            double sum = DoubleStream.of(row).sum();
            System.out.println("row " + i + " prob sum = " + sum);
            assertEquals(1, sum, 1e-15);
        }

//        for (int i=0; i < prob.length; i++)
//            assertTrue(prob[i] > 0);

        assertArrayEquals(new double[]{
                0.667487172482777, 0.22927797022595872, 0.10323485729126429,
                0.22927797022595856, 0.4154009466133881, 0.3553210831606529,
                0.1032348572912642, 0.355321083160653, 0.5414440595480825}, prob, 1e-15);
    }
}
