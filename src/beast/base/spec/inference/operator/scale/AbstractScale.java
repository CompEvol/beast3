package beast.base.spec.inference.operator.scale;

import beast.base.core.Input;
import beast.base.inference.operator.kernel.KernelOperator;
import beast.base.spec.inference.operator.AutoOptimized;
import beast.base.util.Randomizer;

import java.text.DecimalFormat;

public abstract class AbstractScale extends KernelOperator implements AutoOptimized {
    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor", "scaling factor: range from 0 to 1. Close to zero is very large jumps, close to 1.0 is very small jumps.", 0.75);
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);
    final public Input<Double> scaleUpperLimit = new Input<>("upper", "Upper Limit of scale factor", 1.0 - 1e-8);
    final public Input<Double> scaleLowerLimit = new Input<>("lower", "Lower limit of scale factor", 1e-8);
    /**
     * shadows input *
     */
    protected double scaleFactor;
    protected double upper;
    protected double lower;

    @Override
    public void initAndValidate() {

        scaleFactor = scaleFactorInput.get();
//        m_bIsTreeScaler = (treeInput.get() != null);
        upper = scaleUpperLimit.get();
        lower = scaleLowerLimit.get();

//        final BooleanParameter indicators = indicatorInput.get();
//        if (indicators != null) {
//            if (m_bIsTreeScaler) {
//                throw new IllegalArgumentException("indicator is specified which has no effect for scaling a tree");
//            }
//            final int dataDim = parameterInput.get().getDimension();
//            final int indsDim = indicators.getDimension();
//            if (!(indsDim == dataDim || indsDim + 1 == dataDim)) {
//                throw new IllegalArgumentException("indicator dimension not compatible from parameter dimension");
//            }
//        }
    }

    //TODO replaced by isValid()
//    protected boolean outsideBounds(final double value, final RealScalarParam param) {
//        final Double l = param.getLower();
//        final Double h = param.getUpper();
//
//        return (value < l || value > h);
        //return (l != null && value < l || h != null && value > h);
//    }

    protected double getScaler() {
        return (scaleFactor + (Randomizer.nextDouble() * ((1.0 / scaleFactor) - scaleFactor)));
    }

    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(final double logAlpha) {
        if (optimiseInput.get()) {
            double delta = calcDelta(logAlpha);
            delta += Math.log(1.0 / scaleFactor - 1.0);
            setCoercableParameterValue(1.0 / (Math.exp(delta) + 1.0));
        }
    }

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        scaleFactor = Math.max(Math.min(value, upper), lower);
    }

    @Override
    public double getTargetAcceptanceProbability() {
        return AutoOptimized.Target_Acceptance_Probability;
    }

    @Override
    public String getPerformanceSuggestion() {
        return getPerformanceSuggestion(this, "scaleFactor");
    }

}
