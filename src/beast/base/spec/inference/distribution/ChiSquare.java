package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.type.IntScalar;
import org.apache.commons.statistics.distribution.ChiSquaredDistribution;


@Description("Chi square distribution, f(x; k) = \\frac{1}{2^{k/2}Gamma(k/2)} x^{k/2-1} e^{-x/2} " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class ChiSquare extends RealTensorDistribution<NonNegativeReal> {
    final public Input<IntScalar<PositiveInt>> dfInput = new Input<>("df",
            "Degrees of freedom, defaults to 1");

    private ChiSquaredDistribution dist = ChiSquaredDistribution.of(1);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        int dF = (dfInput.get() != null) ? dfInput.get().get() : 1;
        if (dist.getDegreesOfFreedom() != dF)
            dist = ChiSquaredDistribution.of(dF);
    }

    @Override
    public ChiSquaredDistribution getDistribution() {
        refresh();
        return dist;
    }

} // class ChiSquare
