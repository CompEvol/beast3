package beast.base.spec.inference.operator.deltaexchange;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.util.Randomizer;

@Description("Delta exchange operator that proposes through a Bactrian distribution for real valued parameters")
public class RealDeltaExchangeOperator extends AbstractDeltaExchange {

    public final Input<RealVectorParam<? extends Real>> parameterInput = new Input<>("parameter",
            "if specified, this parameter is operated on",
            Input.Validate.REQUIRED, RealVectorParam.class);


    public void initAndValidate() {
        super.initAndValidate();

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
        final int dim = parameterInput.get().size();
        int[] parameterWeights = getWeights(dim);
//        final int dim = parameterWeights.length;

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

}
