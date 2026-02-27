package beast.base.spec.evolution.tree.coalescent;


import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.tree.coalescent.PopulationFunction;
import beast.base.inference.CalculationNode;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;

import java.util.List;


/**
 * Wrapper that scales a population function by a constant multiplicative factor.
 * Useful for adjusting population sizes across loci with different ploidy levels.
 *
 * @author Joseph Heled
 */
@Description("Scale a demographic function by a constant factor")
public class ScaledPopulationFunction extends PopulationFunction.Abstract {
    final public Input<PopulationFunction> popParameterInput = new Input<>("population",
            "population function to scale. ", Validate.REQUIRED);

    final public Input<RealScalar<? extends PositiveReal>> scaleFactorInput = new Input<>("factor",
            "scale population by this factor.", Validate.REQUIRED);

    public ScaledPopulationFunction() {
    }

    // Implementation of abstract methods

    @Override
	public List<String> getParameterIds() {
        List<String> ids = popParameterInput.get().getParameterIds();
        if (scaleFactorInput.get() instanceof BEASTInterface beastInterface) {
        	ids.add(beastInterface.getID());
        }
        return ids;
    }

    @Override
	public double getPopSize(double t) {
        return popParameterInput.get().getPopSize(t) * scaleFactorInput.get().get();
    }

    @Override
	public double getIntensity(double t) {
        double intensity = popParameterInput.get().getIntensity(t);
        double scale = scaleFactorInput.get().get();
        return intensity / scale;
    }

    @Override
	public double getInverseIntensity(double x) {
        throw new RuntimeException("unimplemented");
    }

    @Override
    protected boolean requiresRecalculation() {
//        return ((CalculationNode) popParameterInput.get()).isDirtyCalculation()
//                || ((CalculationNode)scaleFactorInput.get()).isDirtyCalculation();
        boolean popDirty = false;
        boolean scaleDirty = false;

        // safe code to use instanceof
        if (popParameterInput.get() instanceof CalculationNode popNode)
            popDirty = popNode.isDirtyCalculation();

        if (scaleFactorInput.get() instanceof CalculationNode scaleNode)
            scaleDirty = scaleNode.isDirtyCalculation();

        return popDirty || scaleDirty;
    }
}
