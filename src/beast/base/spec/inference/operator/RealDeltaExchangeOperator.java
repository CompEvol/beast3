package beast.base.spec.inference.operator;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.operator.kernel.KernelOperator;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.util.Randomizer;

import java.text.DecimalFormat;

@Description("Delta exchange operator that proposes through a Bactrian distribution for real valued parameters")
public class RealDeltaExchangeOperator extends KernelOperator {

    public final Input<RealVectorParam<? extends Real>> parameterInput = new Input<>("parameter",
            "if specified, this parameter is operated on");

    public final Input<Double> deltaInput = new Input<>("delta",
            "Magnitude of change for two randomly picked values.", 1.0);
    public final Input<Boolean> autoOptimizeiInput = new Input<>("autoOptimize",
            "if true, window size will be adjusted during the MCMC run to improve mixing.",
            true);
    public final Input<IntVectorParam<? extends PositiveInt>> parameterWeightsInput = new Input<>(
            "weightvector", "weights on a vector parameter");


    private boolean autoOptimize;
    private double delta;

    private int[] weights() {
        int[] weights = new int[parameterInput.get().size()];

        if (parameterWeightsInput.get() != null) {
            if (weights.length != parameterWeightsInput.get().size())
                throw new IllegalArgumentException(
                        "Weights vector should have the same length as parameter dimension");

            for (int i = 0; i < weights.length; i++) {
                weights[i] = parameterWeightsInput.get().getValue(i);
            }
        } else {
            for (int i = 0; i < weights.length; i++) {
                weights[i] = 1;
            }
        }
        return weights;
    }

    public void initAndValidate() {
        super.initAndValidate();

        autoOptimize = autoOptimizeiInput.get();
        delta = deltaInput.get();

        // dimension sanity check
        int dim = parameterInput.get().size();
        if (dim <= 1) {
            //TODO why not Exception?
            Log.warning.println("WARNING: the dimension of the parameter is " + dim + " at the start of the run.\n"
                    + "         The operator " + getID() + " has no effect (if this does not change).");
        }

    }

    @Override
    public final double proposal() {
        int[] parameterWeights = weights();
        final int dim = parameterWeights.length;

        // Find the number of weights that are nonzero
        int nonZeroWeights = 0;
        for (int i: parameterWeights) {
            if (i != 0) {
                ++nonZeroWeights;
            }
        }

        if (nonZeroWeights <= 1) {
            // it is impossible to select two distinct entries in this case, so there is nothing to propose
            return 0.0;
        }

        // Generate indices for the values to be modified
        int dim1 = Randomizer.nextInt(nonZeroWeights);
        int dim2 = Randomizer.nextInt(nonZeroWeights-1);
        if (dim2 >= dim1) {
            ++dim2;
        }
        if (nonZeroWeights<dim) {
            // There are zero weights, so we need to increase dim1 and dim2 accordingly.
            int nonZerosBeforeDim1 = dim1;
            int nonZerosBeforeDim2 = dim2;
            dim1 = 0;
            dim2 = 0;
            while (nonZerosBeforeDim1 > 0 | parameterWeights[dim1] == 0 ) {
                if (parameterWeights[dim1] != 0) {
                    --nonZerosBeforeDim1;
                }
                ++dim1;
            }
            while (nonZerosBeforeDim2 > 0 | parameterWeights[dim2] == 0 ) {
                if (parameterWeights[dim2] != 0) {
                    --nonZerosBeforeDim2;
                }
                ++dim2;
            }
        }

        double logq = 0.0;

        RealVectorParam realparameter = parameterInput.get();

        // operate on real parameter
        double scalar1 = realparameter.getValue(dim1);
        double scalar2 = realparameter.getValue(dim2);

        // exchange a random delta
        final double d = getNextDouble(0);

        if (parameterWeights[dim1] != parameterWeights[dim2]) {
            final double sumW = parameterWeights[dim1] + parameterWeights[dim2];
            scalar1 -= d * parameterWeights[dim2] / sumW;
            scalar2 += d * parameterWeights[dim1] / sumW;
        } else {
            scalar1 -= d / 2; // for equal weights
            scalar2 += d / 2;
        }


        if (scalar1 < realparameter.getLower() || scalar1 > realparameter.getUpper() ||
                scalar2 < realparameter.getLower() || scalar2 > realparameter.getUpper()) {
            logq = Double.NEGATIVE_INFINITY;
        } else {
            realparameter.setValue(dim1, scalar1);
            realparameter.setValue(dim2, scalar2);
        }

        //System.err.println("apply deltaEx");
        // symmetrical move so return a zero hasting ratio
        return logq;
    }

    private double getNextDouble(int i) {
        return kernelDistribution.getRandomDelta(i, Double.NaN, delta);
    }

    @Override
    public double getCoercableParameterValue() {
        return delta;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        delta = value;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    @Override
    public void optimize(final double logAlpha) {
        // must be overridden by operator implementation to have an effect
        if (autoOptimize) {
            double _delta = calcDelta(logAlpha);
            _delta += Math.log(delta);
            delta = Math.exp(_delta);
//            if (isIntegerOperator) {
//            	// when delta < 0.5
//            	// Randomizer.nextInt((int) Math.round(delta)) becomes
//            	// Randomizer.nextInt(0) which results in an exception
//            	delta = Math.max(0.5000000001, delta);
//            }
        }

    }

    @Override
    public final String getPerformanceSuggestion() {
        final double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        final double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        final double newDelta = delta * ratio;

        final DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting delta to about " + formatter.format(newDelta);
        } else if (prob > 0.40) {
            return "Try setting delta to about " + formatter.format(newDelta);
        } else return "";
    }


    @Override
    public double getTargetAcceptanceProbability() {
        return 0.3;
    }

}
