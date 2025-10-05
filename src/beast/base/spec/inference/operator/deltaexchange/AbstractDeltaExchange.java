package beast.base.spec.inference.operator.deltaexchange;

import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.Operator;
import beast.base.spec.domain.Domain;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.VectorParam;
import beast.base.util.Randomizer;

import java.text.DecimalFormat;
import java.util.Arrays;

public abstract class AbstractDeltaExchange extends Operator {

    public final Input<IntVectorParam<? extends PositiveInt>> parameterWeightsInput = new Input<>(
            "weightvector", "weights on a vector parameter");

    public final Input<Double> deltaInput = new Input<>("delta",
            "Magnitude of change for two randomly picked values.", 1.0);
    public final Input<Boolean> autoOptimizeiInput = new Input<>("autoOptimize",
            "if true, window size will be adjusted during the MCMC run to improve mixing.",
            true);

    // default
    protected final double Target_Acceptance_Probability = 0.3;

    protected boolean autoOptimize;
    protected double delta;


    @Override
    public void initAndValidate() {
        delta = deltaInput.get();
        autoOptimize = autoOptimizeiInput.get();

        // dimension sanity check
        int dim = getDimension();
        if (dim <= 1) {
            //TODO why not Exception?
            Log.warning.println("WARNING: the dimension of the parameter is " + dim + " at the start of the run.\n"
                    + "         The operator " + getID() + " has no effect (if this does not change).");
        }

    }

    /**
     * @return the parameter dimension, which should be same as the dimension of weights
     */
    abstract int getDimension();

    protected int[] getWeights(int paramDim) {
        int[] weights = new int[paramDim];

        if (parameterWeightsInput.get() != null) {
            if (weights.length != parameterWeightsInput.get().size())
                throw new IllegalArgumentException(
                        "Weights vector should have the same length as parameter dimension");

            for (int i = 0; i < weights.length; i++) {
                weights[i] = parameterWeightsInput.get().get(i);
            }
        } else {
            Arrays.fill(weights, 1);
        }
        return weights;
    }

    protected record IntPair(int first, int second) {}

    /**
     * Generate a pair of indices for the values to be modified.
     * @param weights  parameter weights
     * @return  a {@link IntPair} of indices of weights,
     *          and use {@link IntPair#first()} and {@link IntPair#second()} to get the value.
     *          If it is impossible to select two distinct entries in this case,
     *          then return null.
     */
    protected IntPair getPairedDim(int[] weights) {

        // Find the number of weights that are nonzero
        int nonZeroWeights = 0;
        for (int i: weights) {
            if (i != 0) {
                ++nonZeroWeights;
            }
        }

        if (nonZeroWeights <= 1) {
            // it is impossible to select two distinct entries in this case, so there is nothing to propose
            return null;
        }

        final int dim = weights.length;
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
            while (nonZerosBeforeDim1 > 0 | weights[dim1] == 0 ) {
                if (weights[dim1] != 0) {
                    --nonZerosBeforeDim1;
                }
                ++dim1;
            }
            while (nonZerosBeforeDim2 > 0 | weights[dim2] == 0 ) {
                if (weights[dim2] != 0) {
                    --nonZerosBeforeDim2;
                }
                ++dim2;
            }
        }

        return new IntPair(dim1, dim2);
    }

    protected <D extends Domain<Double>> double proposeReal(VectorParam<D,Double> realVectorParam) {
        final int dim = getDimension();
        int[] parameterWeights = getWeights(dim);
//        final int dim = parameterWeights.length;

        // Generate indices for the values to be modified
        IntPair dims = getPairedDim(parameterWeights);
        // it is impossible to select two distinct entries in this case, so there is nothing to propose
        if (dims == null) return 0.0;

        int dim1 = dims.first();
        int dim2 = dims.second();

        double logq = 0.0;

        // operate on real parameter
        double scalar1 = realVectorParam.get(dim1);
        double scalar2 = realVectorParam.get(dim2);

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

        if ( !realVectorParam.isValid(scalar1) ||  ! realVectorParam.isValid(scalar2) ) {
            logq = Double.NEGATIVE_INFINITY;
        } else {
            realVectorParam.set(dim1, scalar1);
            realVectorParam.set(dim2, scalar2);
        }

        //System.err.println("apply deltaEx");
        // symmetrical move so return a zero hasting ratio
        return logq;
    }

    abstract double getNextDouble(int i);

    // TODO do we need it?
//    abstract int getNextInt(int i);

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
        }

    }

    @Override
    public final String getPerformanceSuggestion() {
        final double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        final double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new delta
        final double newDelta = getCoercableParameterValue() * ratio;

        final DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting delta to about " + formatter.format(newDelta);
        } else if (prob > 0.40) {
            return "Try setting delta to about " + formatter.format(newDelta);
        } else return "";
    }

    @Override
    public double getCoercableParameterValue() {
        return delta;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        delta = value;
    }

    @Override
    public double getTargetAcceptanceProbability() {
        return Target_Acceptance_Probability;
    }

}
