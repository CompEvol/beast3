package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ChiSquaredDistribution;


@Description("Chi square distribution, f(x; k) = \\frac{1}{2^{k/2}Gamma(k/2)} x^{k/2-1} e^{-x/2} " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class ChiSquare extends RealTensorDistribution<RealScalar<NonNegativeReal>, NonNegativeReal> {

    // Note Apache Commons Statistics uses double for Chi-squared degrees of freedom,
    // because it is a special case of the Gamma distribution.
    final public Input<RealScalar<PositiveReal>> dfInput = new Input<>("df",
            "Degrees of freedom, defaults to 1");

    protected ChiSquaredDistribution dist = ChiSquaredDistribution.of(1);

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public ChiSquare() {
    }

    public ChiSquare(RealScalar<NonNegativeReal> param, RealScalar<PositiveReal> df) {

        try {
            initByName("param", param, "df", df);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
        super.initAndValidate();
    }

    /**
     * make sure internal state is up to date *
     */
	void refresh() {
        double dF = (dfInput.get() != null) ? dfInput.get().get() : 1;
        if (dist.getDegreesOfFreedom() != dF)
            dist = ChiSquaredDistribution.of(dF);
    }

    @Override
    protected ChiSquaredDistribution getDistribution() {
        refresh();
        return dist;
    }

    @Override
    protected RealScalar<NonNegativeReal> valueToTensor(double value) {
        return new RealScalarParam<>(value, NonNegativeReal.INSTANCE);
    }

} // class ChiSquare
