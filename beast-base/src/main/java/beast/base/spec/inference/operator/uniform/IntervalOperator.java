package beast.base.spec.inference.operator.uniform;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Scalable;
import beast.base.inference.operator.kernel.KernelOperator;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.util.Randomizer;

import java.text.DecimalFormat;

@Description("A Bactrian interval operator applies standard Hastings scaling move to a parameter that is bounded " +
        "between (lower, upper). The transformation is undefined when the parameter value lies exactly on either boundary." +
        "For vector-valued parameters, a random dimension is selected and scaled by a random factor " +
        "drawn from a Bactrian distribution, ensuring that the proposed value remains within the valid range."
        + "This operator is generally more efficient than the standard UniformOperator")
public class IntervalOperator extends KernelOperator {

    final public Input<Scalable> parameterInput = new Input<>("parameter",
            "the parameter to operate a random walk on.", Validate.REQUIRED);

    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor",
            "scaling factor: larger means more bold proposals", 1.0);
    final public Input<Boolean> optimiseInput = new Input<>("optimise",
            "flag to indicate that the scale factor is automatically changed in order to " +
                    "achieve a good acceptance rate (default true)", true);

    double scaleFactor;
    double lower, upper;

    // default
    protected final double Target_Acceptance_Probability = 0.3;

    @Override
    public void initAndValidate() {
        super.initAndValidate();

        Scalable param = parameterInput.get();
        if (! (param instanceof RealVectorParam<?> || param instanceof RealScalarParam<?>) )
            throw new IllegalArgumentException("The parameter for ScaleOperator must be a RealScalarParam or RealVectorParam ! But " + param.getClass());

        scaleFactor = scaleFactorInput.get();

        lower = getPamaLower();
        upper = getPamaUpper();

        if (Double.isInfinite(lower)) {
            throw new IllegalArgumentException("Lower bound should be finite");
        }
        if (Double.isInfinite(upper)) {
            throw new IllegalArgumentException("Upper bound should be finite");
        }

    }

    private double getPamaLower() {
        Scalable param = parameterInput.get();

        if (param instanceof RealScalarParam<?> realScalarParam) {
            double lower = realScalarParam.getLower();
            // Ensure that the value is not sitting on the limit (due to numerical issues for example)
            if (realScalarParam.get() <= lower)
                throw new IllegalArgumentException("Value cannot be smaller than and equal to lower ! value = " +
                        realScalarParam.get() + ", lower = " + lower );
            return lower;

        } else if (param instanceof RealVectorParam<?> realVectorParam) {
            double lower = realVectorParam.getLower();
            for (int i = 0; i < realVectorParam.size(); i++) {
                if (realVectorParam.get(i) <= lower)
                    throw new IllegalArgumentException("Value cannot be smaller than and equal to lower ! value = " +
                            realVectorParam.get(i) + ", lower = " + lower );
            }
            return lower;

        }
        throw new IllegalArgumentException("The parameter for ScaleOperator must be a RealScalarParam or RealVectorParam !");
    }

    private double getPamaUpper() {
        Scalable param = parameterInput.get();

        if (param instanceof RealScalarParam<?> realScalarParam) {
            double upper = realScalarParam.getUpper();
            // Ensure that the value is not sitting on the limit (due to numerical issues for example)
            if (realScalarParam.get() >= upper)
                throw new IllegalArgumentException("Value cannot be smaller than and equal to lower ! value = " +
                        realScalarParam.get() + ", upper = " + upper );
            return upper;

        } else if (param instanceof RealVectorParam<?> realVectorParam) {
            double upper = realVectorParam.getUpper();
            for (int i = 0; i < realVectorParam.size(); i++) {
                if (realVectorParam.get(i) >= upper)
                    throw new IllegalArgumentException("Value cannot be bigger than and equal to upper ! value = " +
                            realVectorParam.get(i) + ", upper = " + upper );
            }
            return upper;

        }
        throw new IllegalArgumentException("The parameter for ScaleOperator must be a RealScalarParam or RealVectorParam !");
    }

    @Override
    public double proposal() {

        Scalable param = parameterInput.get();

        final double value;
        int index = 0; // default for scalar case
        // Identify parameter type
        if (param instanceof RealScalarParam<?> rs) {
            value = rs.get();
        } else if (param instanceof RealVectorParam<?> rv) {
            index = Randomizer.nextInt(rv.size());
            value = rv.get(index);
        } else
            throw new IllegalArgumentException("The parameter for ScaleOperator must be a RealScalarParam or RealVectorParam !");

        double scale = kernelDistribution.getScaler(index, value, scaleFactor);
        //Proposed transformation
        double y = (upper - value) / (value - lower);
        y *= scale;
        double newValue = (upper + lower * y) / (y + 1.0);

        if (newValue < lower || newValue > upper)
            throw new RuntimeException("programmer error: new value proposed outside range");
        // Ensure that the value is not sitting on the limit (due to numerical issues for example)
        if (newValue == lower || newValue == upper)
            return Double.NEGATIVE_INFINITY;

        if (param instanceof RealScalarParam<?> rs)
            rs.set(newValue);
        else {
            RealVectorParam<?> rv_ = (RealVectorParam<?>) param;
            rv_.set(index, newValue);
        }

        //Hastings ratio
        double logHR = Math.log(scale) + 2.0 * Math.log((newValue - lower)/(value - lower));
        return logHR;
    }

    protected double getScaler(int i, double value) {
        return kernelDistribution.getScaler(i, value, getCoercableParameterValue());
    }

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        scaleFactor = value;
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
        if (optimiseInput.get()) {
            double delta = calcDelta(logAlpha);
            double scaleFactor = getCoercableParameterValue();
            delta += Math.log(scaleFactor);
            scaleFactor = Math.exp(delta);
            setCoercableParameterValue(scaleFactor);
        }
    }

    @Override
    public double getTargetAcceptanceProbability() {
        return Target_Acceptance_Probability;
    }

    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = getCoercableParameterValue() * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Try setting scale factor to about " + formatter.format(newWindowSize);
        } else return "";
    }


} // class BactrianIntervalOperator