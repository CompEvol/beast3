package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.GammaDistribution;


@Description("Gamma distribution. for x>0  g(x;alpha,beta) = 1/Gamma(alpha) beta^alpha} x^{alpha - 1} e^{-\frac{x}{beta}}" +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Gamma extends RealTensorDistribution<NonNegativeReal> {
    final public Input<RealScalar<PositiveReal>> alphaInput = new Input<>("alpha",
            "shape parameter, defaults to 2");
    final public Input<RealScalar<PositiveReal>> betaInput = new Input<>("beta",
            "second parameter depends on mode, defaults to 2."
    		+ "For mode=ShapeScale beta is interpreted as scale. "
    		+ "For mode=ShapeRate beta is interpreted as rate. "
    		+ "For mode=ShapeMean beta is interpreted as mean."
    		+ "For mode=OneParameter beta is ignored.");
    
    public enum mode {ShapeScale, ShapeRate, ShapeMean, OneParameter};
    final public Input<mode> modeInput = new Input<>("mode", 
            "determines parameterisation. "
    		+ "For ShapeScale beta is interpreted as scale. "
    		+ "For ShapeRate beta is interpreted as rate. "
    		+ "For ShapeMean beta is interpreted as mean."
    		+ "For OneParameter beta is ignored.", mode.ShapeScale, mode.values());

    
    GammaDistribution dist = GammaDistribution.of(2.0, 2.0);

    mode parameterisation = mode.ShapeScale;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Gamma() {}

    public Gamma(RealScalar<UnitInterval> tensor,
                 RealScalar<PositiveReal> alpha, RealScalar<PositiveReal> beta) {
        this(tensor, alpha, beta, mode.ShapeScale);
    }

    public Gamma(RealScalar<UnitInterval> tensor,
                RealScalar<PositiveReal> alpha, RealScalar<PositiveReal> beta,
                 mode mode) {

        try {
            initByName("tensor", tensor, "alpha", alpha, "beta", beta,
                    "mode", mode);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
    	parameterisation = modeInput.get();
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        double alpha = (alphaInput.get() != null) ? alphaInput.get().get() : 2.0;
        double beta =  2.0;
        
        switch (parameterisation) {
        case ShapeScale:
            if (betaInput.get() != null) {
                beta = betaInput.get().get();
            }
        	break;
        case ShapeRate:
            if (betaInput.get() != null) {
                beta = 1.0/betaInput.get().get();
            }
        	break;
        case ShapeMean:
            if (betaInput.get() != null) {
                beta = betaInput.get().get() / alpha;
            }
        	break;
        case OneParameter:
        	beta = 1.0 / alpha;
        	break;
        }

        // Floating point comparison
        if (Math.abs(getAlpha() - alpha) > EPS ||  Math.abs(getBeta() - beta) > EPS)
            dist = GammaDistribution.of(alpha, beta);
    }

    @Override
    public GammaDistribution getDistribution() {
        refresh();
        return dist;
    }

    @Override
    public double getMeanWithoutOffset() {
    	refresh();
    	return getAlpha() * getBeta();
    }

    public double getAlpha() {
        return dist.getShape(); // alpha = shape;
    }

    public double getBeta() {
        return 1.0 / dist.getScale(); // beta  = 1.0 / scale;
    }

} // class Gamma
