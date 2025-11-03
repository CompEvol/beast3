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
public class Gamma extends RealTensorDistribution<RealScalar<PositiveReal>, PositiveReal> {

    final public Input<RealScalar<PositiveReal>> alphaInput = new Input<>("alpha",
            "shape parameter, defaults to 1");
    final public Input<RealScalar<PositiveReal>> thetaInput = new Input<>("theta",
            "scale parameter for Shape–Scale form, defaults to 1.", Input.Validate.XOR);
    final public Input<RealScalar<PositiveReal>> betaInput = new Input<>("beta",
            "rate parameter for Shape–Rate form, defaults to 1.", Input.Validate.XOR);

    protected GammaDistribution dist = GammaDistribution.of(1.0, 1.0);

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Gamma() {}

    // default to use theta
    public Gamma(RealScalar<PositiveReal> param,
                 RealScalar<PositiveReal> alpha, RealScalar<PositiveReal> theta) {
        this(param, alpha, theta, null);
    }

    public Gamma(RealScalar<PositiveReal> param,
                 RealScalar<PositiveReal> alpha, RealScalar<PositiveReal> theta,
                 RealScalar<PositiveReal> beta) {

        try {
            if (theta != null && beta == null) {
                initByName("param", param, "alpha", alpha, "theta", theta);
            } else if (beta != null && theta == null) {
                initByName("param", param, "alpha", alpha, "beta", beta);
            } else
                throw new IllegalArgumentException("Must have either theta or beta ! ");
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
        double alpha = (alphaInput.get() != null) ? alphaInput.get().get() : 1.0;

        double scale = 1.0; // default
        if (thetaInput.get() != null && betaInput.get()  == null) {
            // θ provided directly
            scale  = thetaInput.get().get();
        } else if (betaInput.get() != null && thetaInput.get()  == null) {
            // β provided : θ = 1 / β
            scale  = 1.0 / betaInput.get().get() ;

        } else if (thetaInput.get() == null && betaInput.get()  == null) {
            // both null, use default
        } else
            throw new IllegalArgumentException("Must have either theta or beta ! ");

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
    protected double getMeanWithoutOffset() {
    	refresh();
    	return dist.getShape() * dist.getScale();
    }

} // class Gamma
