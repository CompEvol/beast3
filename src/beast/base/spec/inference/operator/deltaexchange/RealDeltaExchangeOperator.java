package beast.base.spec.inference.operator.deltaexchange;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.operator.kernel.KernelDistribution;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;

//TODO cannot use AbstractDeltaExchange

@Description("Delta exchange operator that proposes through a Bactrian distribution for real valued parameters")
public class RealDeltaExchangeOperator extends AbstractDeltaExchange {

    public final Input<RealVectorParam<? extends Real>> parameterInput = new Input<>("parameter",
            "if specified, this parameter is operated on",
            Input.Validate.REQUIRED, RealVectorParam.class);

    public final Input<KernelDistribution> kernelDistributionInput = new Input<>("kernelDistribution", "provides sample distribution for proposals",
            KernelDistribution.newDefaultKernelDistribution());

    protected KernelDistribution kernelDistribution;

    @Override
    public void initAndValidate() {
        kernelDistribution = kernelDistributionInput.get();
        super.initAndValidate();
    }

    @Override
    int getDimension() {
        return parameterInput.get().size();
    }

    @Override
    public final double proposal() {
        return proposeReal(parameterInput.get());
    }

    double getNextDouble(int i) {
        return kernelDistribution.getRandomDelta(i, Double.NaN, delta);
    }

}
