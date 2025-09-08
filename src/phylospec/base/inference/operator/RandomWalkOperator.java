package phylospec.base.inference.operator;

import beast.base.core.Description;
import beast.base.inference.Operator;

import org.phylospec.primitives.PositiveReal;
import org.phylospec.primitives.Real;
import org.phylospec.types.Scalar;
import phylospec.base.core.Input;
import phylospec.base.core.Input.Validate;
import phylospec.base.core.TensorInput;
import phylospec.base.inference.ScalarParam;

import java.text.DecimalFormat;


@Description("A random walk operator that selects a random dimension of the real parameter and perturbs the value a " +
        "random amount within +/- windowSize.")
public class RandomWalkOperator<P extends Real> extends Operator {
    final public TensorInput<Scalar<P>> parameterInput = new TensorInput<>(
            "parameter", "the parameter to operate a random walk on.",
            Validate.REQUIRED, Real.INSTANCE);

    final public TensorInput<Scalar<PositiveReal>> windowSizeInput = new TensorInput<>(
            "windowSize", "the size of the window both up and down when using uniform interval OR standard deviation when using Gaussian",
            Validate.REQUIRED);
    final public Input<Boolean> useGaussianInput = new Input<>(
            "useGaussian", "Use Gaussian to move instead of uniform interval. Default false.",
            false);

    double windowSize = 1;
    boolean useGaussian;

    @Override
	public void initAndValidate() {
        if (windowSizeInput.get() != null) // default to 1
            windowSize = windowSizeInput.get().get();
        useGaussian = useGaussianInput.get();
    }

    /**
     * override this for proposals,
     * returns log of hastingRatio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {

//        RealParameter param = (RealParameter) InputUtil.get(parameterInput, this);
        //TODO
        ScalarParam<P> param = (ScalarParam<P>) parameterInput.get();

//        int i = Randomizer.nextInt(param.getDimension());
//        double value = param.get(i);
        double value = param.get();
        Double newValue = OperatorUtils.proposeNewDouble(value, param, useGaussian, windowSize);
        if (newValue == null)
            return Double.NEGATIVE_INFINITY;

        param.set(newValue);

        return 0.0;
    }

    @Override
    public double getCoercableParameterValue() {
        return windowSize;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        windowSize = value;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
        double delta = calcDelta(logAlpha);

        delta += Math.log(windowSize);
        windowSize = Math.exp(delta);
    }

    @Override
    public final String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = windowSize * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else if (prob > 0.40) {
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else return "";
    }
} // class RealRandomWalkOperator