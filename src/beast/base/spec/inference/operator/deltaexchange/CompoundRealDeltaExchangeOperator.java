package beast.base.spec.inference.operator.deltaexchange;

import beast.base.core.Input;
import beast.base.inference.operator.kernel.KernelDistribution;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.operator.CompoundRealScalarParamHelper;
import beast.base.spec.inference.parameter.RealScalarParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompoundRealDeltaExchangeOperator extends AbstractDeltaExchange {

    public final Input<List<RealScalarParam<? extends Real>>> parameterInput = new Input<>("parameter",
            "if specified, this parameter is operated on",
            new ArrayList<>(), Input.Validate.REQUIRED, ArrayList.class);

    public final Input<KernelDistribution> kernelDistributionInput = new Input<>("kernelDistribution", "provides sample distribution for proposals",
            KernelDistribution.newDefaultKernelDistribution());

    protected KernelDistribution kernelDistribution;

    private CompoundRealScalarParamHelper<? extends Real> compoundParameter;

    @Override
    public void initAndValidate() {
        kernelDistribution = kernelDistributionInput.get();

        // create single parameter from the list of int-parameters
        compoundParameter = new CompoundRealScalarParamHelper(parameterInput.get());

        super.initAndValidate();
    }

    @Override
    int getDimension() {
        return parameterInput.get().size();
    }

    @Override
    public final double proposal() {
        return proposeReal(Objects.requireNonNull(compoundParameter));
    }

    double getNextDouble(int i) {
        return kernelDistribution.getRandomDelta(i, Double.NaN, delta);
    }

}
