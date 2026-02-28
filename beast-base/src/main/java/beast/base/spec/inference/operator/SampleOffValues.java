package beast.base.spec.inference.operator;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Operator;
import beast.base.inference.util.InputUtil;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.Scalar;
import beast.base.util.Randomizer;

/**
 * Operator that samples parameter values from a specified distribution.
 * Uses an indicator vector to determine which elements are resampled,
 * leaving others unchanged.
 *
 * @author Joseph Heled
 */
@Description("Sample values from a distribution")
public class SampleOffValues extends Operator {
    final public Input<RealVectorParam<? extends Real>> valuesInput = new Input<>("values", "vector of target values", Input.Validate.REQUIRED);

    final public Input<BoolVectorParam> indicatorsInput = new Input<>("indicators", "Sample only entries which are 'off'");

    final public Input<ScalarDistribution<Scalar<Real,Double>, Double>> distInput = new Input<>("dist",
            "distribution to sample from.", Input.Validate.REQUIRED);

    public final Input<Boolean> scaleAll =
            new Input<>("all", "if true, sample all off values in one go.", false);

    @Override
	public void initAndValidate() {
    }

    @Override
    public double proposal() {
        final BoolVectorParam indicators = (BoolVectorParam) InputUtil.get(indicatorsInput, this);
        final RealVectorParam<? extends Real> data = (RealVectorParam<? extends Real>) InputUtil.get(valuesInput, this);
        final ScalarDistribution<Scalar<Real,Double>, Double> distribution = distInput.get();

        final int idim = indicators.size();

        final int offset = (data.size() - 1) == idim ? 1 : 0;
        assert offset == 1 || data.size() == idim : "" + idim + " (?+1) != " + data.size();

        double hr = Double.NEGATIVE_INFINITY;

        if( scaleAll.get() ) {
            for (int i = offset; i < idim; ++i) {
                if( !indicators.get(i-offset) ) {
                    try {
                        final double val = distribution.inverseCumulativeProbability(Randomizer.nextDouble());
                        hr += distribution.logDensity(data.get(i));
                        data.set(i, val);
                    } catch (Exception e) {
                        // some distributions fail on extreme values - currently gamma
                        return Double.NEGATIVE_INFINITY;
                    }
                }
            }
        } else {

            // available locations for direct sampling
            int[] loc = new int[idim];
            int locIndex = 0;

            for (int i = 0; i < idim; ++i) {
                if( !indicators.get(i) ) {
                    loc[locIndex] = i + offset;
                    ++locIndex;
                }
            }

            if( locIndex > 0 ) {
                final int index = loc[Randomizer.nextInt(locIndex)];
                try {
                    final double val = distribution.inverseCumulativeProbability(Randomizer.nextDouble());
                    hr = distribution.logDensity(data.get(index));
                    data.set(index, val);
                } catch (Exception e) {
                    // some distributions fail on extreme values - currently gamma
                    return Double.NEGATIVE_INFINITY;
                    //throw new OperatorFailedException(e.getMessage());
                }
            } else {
                // no non-active indicators
                //return Double.NEGATIVE_INFINITY;
            }
        }
        return hr;
    }
}
