package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.GammaDistribution;


@Description("Gamma distribution. for x>0  g(x;alpha,beta) = 1/Gamma(alpha) beta^alpha} x^{alpha - 1} e^{-\frac{x}{beta}}" +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class GammaMean extends RealTensorDistribution<RealScalar<PositiveReal>, PositiveReal> {

    final public Input<RealScalar<PositiveReal>> alphaInput = new Input<>("alpha",
            "shape parameter, defaults to 2", Input.Validate.REQUIRED);
    final public Input<RealScalar<PositiveReal>> meanInput = new Input<>("mean",
            "the expected mean of Gamma distribution, which equals shape * scale. " +
                    "If it is not given, then use mean = 1.0 (default) which is equivalent to ignoring scale parameter.",
            Input.Validate.OPTIONAL);

    protected GammaDistribution dist = GammaDistribution.of(2.0, 0.5);

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public GammaMean() {}

    // default to use theta
    public GammaMean(RealScalar<PositiveReal> param,
                     RealScalar<PositiveReal> alpha, RealScalar<PositiveReal> mean) {
        try {
            initByName("param", param, "alpha", alpha, "mean", mean);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
    	refresh();
    }

    /**
     * make sure internal state is up to date *
     */
	void refresh() {
        double alpha = (alphaInput.get() != null) ? alphaInput.get().get() : 2.0;
        double mean = (meanInput.get() != null) ? meanInput.get().get() : 1.0; // default

        double scale = mean / alpha;

        // Floating point comparison
        if (Math.abs(dist.getShape() - alpha) > EPS ||  Math.abs(dist.getScale() - scale) > EPS)
            dist = GammaDistribution.of(alpha, scale);
    }

    @Override
    protected GammaDistribution getDistribution() {
        refresh();
        return dist;
    }

    @Override
    protected RealScalar<PositiveReal> valueToTensor(double value) {
        return new RealScalarParam<>(value, PositiveReal.INSTANCE);
    }

    @Override
    public double getMeanWithoutOffset() {
    	refresh();
    	return dist.getShape() * dist.getScale();
    }

} // class Gamma
